// pagely-auth-service/src/test/java/com/pagely/authservice/infrastructure/jwt/JjwtTokenProviderTest.java

package com.pagely.authservice.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pagely.authservice.domain.exception.AuthErrorCode;
import com.pagely.authservice.domain.model.AccessToken;
import com.pagely.authservice.domain.model.RefreshToken;
import com.pagely.authservice.domain.model.TokenPayload;
import com.pagely.common.auth.Role;
import com.pagely.common.exception.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JjwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-at-least-256bits-bytes";
    private static final String ISSUER = "pagely-test";

    private final JjwtTokenProvider provider = createProvider(30, 14);

    private JjwtTokenProvider createProvider(int atMinutes, int rtDays) {
        return new JjwtTokenProvider(
                new JwtProperties(
                        SECRET,
                        ISSUER,
                        new JwtProperties.AccessToken(atMinutes),
                        new JwtProperties.RefreshToken(rtDays)
                )
        );
    }

    @Nested
    @DisplayName("AT 발급")
    class CreateAccessToken {

        @Test
        @DisplayName("정상 발급 시 토큰 + 만료 시각이 채워진다")
        void issue() {
            UUID userId = UUID.randomUUID();

            AccessToken token = provider.createAccessToken(userId, Role.USER);

            assertThat(token.value()).isNotBlank();
            assertThat(token.issuedAt()).isNotNull();
            assertThat(token.expiresAt()).isAfter(token.issuedAt());
            assertThat(token.expiresAt().toEpochMilli() - token.issuedAt().toEpochMilli())
                    .isBetween(29 * 60_000L, 31 * 60_000L); // ≈ 30분
        }

        @Test
        @DisplayName("userId 가 null 이면 예외")
        void requireUserId() {
            assertThatThrownBy(() -> provider.createAccessToken(null, Role.USER))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("role 이 null 이면 예외")
        void requireRole() {
            assertThatThrownBy(() -> provider.createAccessToken(UUID.randomUUID(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("RT 발급")
    class CreateRefreshToken {

        @Test
        @DisplayName("RT 는 의미 없는 랜덤 문자열")
        void issue() {
            RefreshToken rt = provider.createRefreshToken();

            assertThat(rt.value()).isNotBlank();
            assertThat(rt.value().length()).isGreaterThan(40); // ~43 chars (256bit Base64URL)
            assertThat(rt.issuedAt()).isNotNull();
            assertThat(rt.expiresAt()).isAfter(rt.issuedAt());
        }

        @Test
        @DisplayName("RT 는 호출마다 다른 값")
        void uniqueness() {
            RefreshToken a = provider.createRefreshToken();
            RefreshToken b = provider.createRefreshToken();

            assertThat(a.value()).isNotEqualTo(b.value());
        }
    }

    @Nested
    @DisplayName("AT 검증")
    class ValidateAccessToken {

        @Test
        @DisplayName("정상 AT 는 payload 추출")
        void valid() {
            UUID userId = UUID.randomUUID();
            AccessToken token = provider.createAccessToken(userId, Role.USER);

            TokenPayload payload = provider.validateAndExtract(token.value());

            assertThat(payload.userId()).isEqualTo(userId);
            assertThat(payload.role()).isEqualTo(Role.USER);
            assertThat(payload.jti()).isNotBlank();
            assertThat(payload.issuedAt()).isNotNull();
            assertThat(payload.expiresAt()).isAfter(payload.issuedAt());
        }

        @Test
        @DisplayName("null / blank 는 INVALID_ACCESS_TOKEN")
        void blank() {
            assertThatThrownBy(() -> provider.validateAndExtract(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.INVALID_ACCESS_TOKEN);

            assertThatThrownBy(() -> provider.validateAndExtract(""))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("형식 깨진 토큰은 INVALID_ACCESS_TOKEN")
        void malformed() {
            assertThatThrownBy(() -> provider.validateAndExtract("not.a.jwt"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("다른 secret 으로 서명된 토큰은 INVALID_ACCESS_TOKEN")
        void wrongSignature() {
            String otherSecret = "another-secret-that-is-long-enough-for-hs256-256bit-please-please";
            SecretKey otherKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));
            Instant now = Instant.now();
            String forged = Jwts.builder()
                    .issuer(ISSUER)
                    .subject(UUID.randomUUID().toString())
                    .claim("role", "USER")
                    .id(UUID.randomUUID().toString())
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(60)))
                    .signWith(otherKey, Jwts.SIG.HS256)
                    .compact();

            assertThatThrownBy(() -> provider.validateAndExtract(forged))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("다른 issuer 토큰은 INVALID_ACCESS_TOKEN")
        void wrongIssuer() {
            JjwtTokenProvider other = new JjwtTokenProvider(
                    new JwtProperties(
                            SECRET,
                            "other-issuer",   // 다른 issuer
                            new JwtProperties.AccessToken(30),
                            new JwtProperties.RefreshToken(14)
                    )
            );
            String token = other.createAccessToken(UUID.randomUUID(), Role.USER).value();

            assertThatThrownBy(() -> provider.validateAndExtract(token))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("만료된 토큰은 EXPIRED_ACCESS_TOKEN")
        void expired() throws InterruptedException {
            // 즉시 만료 (0분으로는 못 만드니 직접 발급)
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            Instant now = Instant.now();
            String expired = Jwts.builder()
                    .issuer(ISSUER)
                    .subject(UUID.randomUUID().toString())
                    .claim("role", "USER")
                    .id(UUID.randomUUID().toString())
                    .issuedAt(Date.from(now.minusSeconds(120)))
                    .expiration(Date.from(now.minusSeconds(60)))  // 이미 만료
                    .signWith(key, Jwts.SIG.HS256)
                    .compact();

            assertThatThrownBy(() -> provider.validateAndExtract(expired))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.EXPIRED_ACCESS_TOKEN);
        }
    }
}
