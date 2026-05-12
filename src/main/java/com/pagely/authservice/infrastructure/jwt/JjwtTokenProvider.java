package com.pagely.authservice.infrastructure.jwt;

import com.pagely.authservice.domain.exception.AuthErrorCode;
import com.pagely.authservice.domain.model.AccessToken;
import com.pagely.authservice.domain.model.RefreshToken;
import com.pagely.authservice.domain.model.TokenPayload;
import com.pagely.authservice.domain.service.JwtTokenProvider;
import com.pagely.common.auth.Role;
import com.pagely.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JjwtTokenProvider implements JwtTokenProvider {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32; // 256bit
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JjwtTokenProvider(JwtProperties properties) {
        // HS256 은 최소 256bit (32bytes) 키 필요
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret 은 최소 32 bytes (256 bit) 이상이어야 합니다. "
                            + "현재: " + secretBytes.length + " bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = properties.issuer();
        this.accessTokenExpirationMs = properties.accessToken().toDuration().toMillis();
        this.refreshTokenExpirationMs = properties.refreshToken().toDuration().toMillis();
    }

    // ====================================================================
    // 발급
    // ====================================================================

    @Override
    public AccessToken createAccessToken(UUID userId, Role role) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 는 필수입니다.");
        }
        if (role == null) {
            throw new IllegalArgumentException("role 은 필수입니다.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(accessTokenExpirationMs);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim(CLAIM_ROLE, role.name())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, SIG.HS256)
                .compact();

        return new AccessToken(token, now, expiresAt);
    }

    @Override
    public RefreshToken createRefreshToken() {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshTokenExpirationMs);

        // 256bit 랜덤 → Base64URL (URL-safe, 44 chars without padding)
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        return new RefreshToken(value, now, expiresAt);
    }

    // ====================================================================
    // 검증
    // ====================================================================

    @Override
    public TokenPayload validateAndExtract(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            String jti = claims.getId();
            Instant issuedAt = claims.getIssuedAt().toInstant();
            Instant expiresAt = claims.getExpiration().toInstant();

            return new TokenPayload(userId, role, jti, issuedAt, expiresAt);

        } catch (ExpiredJwtException e) {
            log.debug("AT 만료: jti={}", e.getClaims().getId());
            throw new BusinessException(AuthErrorCode.EXPIRED_ACCESS_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("AT 검증 실패: {}", e.getMessage());
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }
    }
}
