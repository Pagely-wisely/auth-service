package com.pagely.authservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.pagely.authservice.domain.model.RefreshToken;
import com.pagely.authservice.domain.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
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
    @DisplayName("RT 만으로 유저 ID 를 성공적으로 조회 (역방향 인덱스 검증)")
    void findUserIdByToken() {
        repository.save(userId, token);

        Optional<UUID> foundUserId = repository.findUserIdByToken(token.value());

        assertThat(foundUserId).isPresent();
        assertThat(foundUserId.get()).isEqualTo(userId);
    }

    @Test
    @DisplayName("잘못된 토큰으로는 유저 ID 조회 실패")
    void findUserIdByWrongToken() {
        repository.save(userId, token);

        Optional<UUID> foundUserId = repository.findUserIdByToken("wrong-token");

        assertThat(foundUserId).isEmpty();
    }

    @Test
    @DisplayName("삭제 시 순방향 키와 역방향 인덱스가 모두 제거됨")
    void deleteRemovesBothKeys() {
        repository.save(userId, token);
        String tokenHash = TokenHasher.hash(token.value());
        String userKey = RedisKeyBuilder.refreshToken(userId);
        String indexKey = RedisKeyBuilder.refreshTokenIndex(tokenHash);

        boolean deleted = repository.delete(userId);

        assertThat(deleted).isTrue();
        assertThat(redisTemplate.hasKey(userKey)).isFalse();
        assertThat(redisTemplate.hasKey(indexKey)).isFalse(); // 인덱스도 반드시 삭제되어야 함
    }

    @Test
    @DisplayName("동일 유저가 새 토큰 발급 시 기존 토큰의 인덱스도 관리되어야 함 (덮어쓰기 검증)")
    void overwriteCleansUpOldIndex() {
        repository.save(userId, token);
        String oldTokenHash = TokenHasher.hash(token.value());
        String oldIndexKey = RedisKeyBuilder.refreshTokenIndex(oldTokenHash);

        RefreshToken newToken = new RefreshToken(
                "new-different-token-value",
                Instant.now(),
                Instant.now().plusSeconds(60)
        );

        repository.save(userId, newToken);

        // 1. 역방향 조회 시 새 유저 ID 가 나와야 함
        assertThat(repository.findUserIdByToken(newToken.value())).contains(userId);
        // 2. 옛날 토큰으로는 더 이상 유저를 찾을 수 없음
        assertThat(repository.findUserIdByToken(token.value())).isEmpty();
        // 3. 레디스 레벨에서 옛 인덱스 키가 삭제되었는지 확인 (Repository save 로직에 따라 다를 수 있음)
        assertThat(redisTemplate.hasKey(oldIndexKey)).isFalse();
    }

    @Test
    @DisplayName("순방향/역방향 키가 모두 해시를 기반으로 정상 저장됨")
    void storageIntegrity() {
        repository.save(userId, token);
        String tokenHash = TokenHasher.hash(token.value());

        String userKey = RedisKeyBuilder.refreshToken(userId);
        String indexKey = RedisKeyBuilder.refreshTokenIndex(tokenHash);

        assertThat(redisTemplate.opsForValue().get(userKey)).isEqualTo(tokenHash);
        assertThat(redisTemplate.opsForValue().get(indexKey)).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("이미 만료된 토큰은 인덱스도 생성되지 않음")
    void expiredTokenNoIndex() {
        Instant past = Instant.now().minusSeconds(60);
        RefreshToken expired = new RefreshToken("expired-token", past, past);

        repository.save(userId, expired);

        assertThat(repository.findUserIdByToken(expired.value())).isEmpty();
        String indexKey = RedisKeyBuilder.refreshTokenIndex(TokenHasher.hash(expired.value()));
        assertThat(redisTemplate.hasKey(indexKey)).isFalse();
    }
}
