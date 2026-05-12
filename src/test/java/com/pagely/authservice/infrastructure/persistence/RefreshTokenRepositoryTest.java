// pagely-auth-service/src/test/java/com/pagely/authservice/infrastructure/persistence/RefreshTokenRepositoryTest.java

package com.pagely.authservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.pagely.authservice.domain.model.RefreshToken;
import com.pagely.authservice.domain.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class RefreshTokenRepositoryTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    RefreshTokenRepository repository;

    @Autowired
    StringRedisTemplate redisTemplate;

    UUID userId;
    RefreshToken token;

    @BeforeEach
    void setUp() {
        // 테스트마다 깨끗한 상태
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        userId = UUID.randomUUID();
        Instant now = Instant.now();
        token = new RefreshToken(
                "DXjQz5nNX7Yl2gv7cKbm3HzBp8QmRtV-test-token",
                now,
                now.plusSeconds(60)   // 1분 만료
        );
    }

    @Test
    @DisplayName("저장한 RT 는 정상 검증")
    void saveAndValidate() {
        repository.save(userId, token);

        boolean result = repository.validate(userId, token.value());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("다른 RT 로는 검증 실패")
    void wrongToken() {
        repository.save(userId, token);

        boolean result = repository.validate(userId, "wrong-token");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("저장 안 한 사용자는 검증 실패")
    void notSaved() {
        UUID anotherUserId = UUID.randomUUID();

        boolean result = repository.validate(anotherUserId, token.value());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("삭제 후 검증 실패")
    void deleteThenValidate() {
        repository.save(userId, token);

        boolean deleted = repository.delete(userId);
        boolean validated = repository.validate(userId, token.value());

        assertThat(deleted).isTrue();
        assertThat(validated).isFalse();
    }

    @Test
    @DisplayName("같은 userId 로 새 RT 저장 시 기존 덮어씀")
    void overwrite() {
        repository.save(userId, token);

        RefreshToken newToken = new RefreshToken(
                "new-different-token-value-test",
                Instant.now(),
                Instant.now().plusSeconds(60)
        );
        repository.save(userId, newToken);

        // 옛 토큰은 거부
        assertThat(repository.validate(userId, token.value())).isFalse();
        // 새 토큰은 통과
        assertThat(repository.validate(userId, newToken.value())).isTrue();
    }

    @Test
    @DisplayName("평문이 아닌 해시로 저장됨 (보안 검증)")
    void hashedStorage() {
        repository.save(userId, token);

        String key = RedisKeyBuilder.refreshToken(userId);
        String stored = redisTemplate.opsForValue().get(key);

        // 평문이 아님
        assertThat(stored).isNotEqualTo(token.value());
        // 해시 형태
        assertThat(stored).isEqualTo(TokenHasher.hash(token.value()));
    }

    @Test
    @DisplayName("이미 만료된 RT 저장 시도는 무시됨")
    void rejectExpiredToken() {
        Instant past = Instant.now().minusSeconds(60);
        RefreshToken expired = new RefreshToken("expired-token", past, past);

        repository.save(userId, expired);

        assertThat(repository.validate(userId, expired.value())).isFalse();
    }
}
