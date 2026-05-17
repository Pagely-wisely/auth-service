package com.pagely.authservice.infrastructure.persistence;

import com.pagely.authservice.domain.model.RefreshToken;
import com.pagely.authservice.domain.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis 기반 RT 화이트리스트 저장소.
 *
 * <p><b>저장 구조</b></p>
 * <ul>
 * <li><b>Forward Index (User to Token):</b> {@code auth-service:rt:{userId}} -> {@code tokenHash}</li>
 * <li><b>Reverse Index (Token to User):</b> {@code auth-service:rt:index:{tokenHash}} -> {@code userId}</li>
 * <li><b>TTL:</b> RT 만료 시각에 맞춰 두 키 모두 자동 삭제</li>
 * </ul>
 *
 * <p><b>설계 의도</b></p>
 * <ul>
 * <li>도메인 모델 (RefreshToken) 과 저장 구조 분리</li>
 * <li>보안을 위해 원문 토큰이 아닌 SHA-256 해시값만 저장</li>
 * <li><b>1:1 관계 유지:</b> 유저당 하나의 RT만 허용하며, 갱신 시 기존 인덱스를 명시적으로 정리 (덮어쓰기 방지)</li>
 * <li>검증 시 O(1) 성능을 위해 역방향 인덱스 활용</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(UUID userId, RefreshToken refreshToken) {
        if (userId == null || refreshToken == null) {
            throw new IllegalArgumentException("userId 와 refreshToken 은 필수입니다.");
        }

        long ttlSeconds = Duration.between(Instant.now(), refreshToken.expiresAt()).toSeconds();
        if (ttlSeconds <= 0) {
            log.warn("이미 만료된 RT 저장 시도 — userId={}", userId);
            return;
        }

        String key = RedisKeyBuilder.refreshToken(userId);
        String tokenHash = TokenHasher.hash(refreshToken.value());
        String indexKey = RedisKeyBuilder.refreshTokenIndex(tokenHash);

        String oldHash = redisTemplate.opsForValue().get(key);
        if (oldHash != null) {
            String oldIndexKey = RedisKeyBuilder.refreshTokenIndex(oldHash);
            redisTemplate.delete(oldIndexKey);
        }

        redisTemplate.opsForValue().set(key, tokenHash, Duration.ofSeconds(ttlSeconds));
        redisTemplate.opsForValue().set(indexKey, userId.toString(), Duration.ofSeconds(ttlSeconds));

        log.debug("RT 저장 완료 — userId={}, ttl={}s", userId, ttlSeconds);
    }

    @Override
    public Optional<UUID> findUserIdByToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        String tokenHash = TokenHasher.hash(rawToken);
        String indexKey = RedisKeyBuilder.refreshTokenIndex(tokenHash);
        String userIdStr = redisTemplate.opsForValue().get(indexKey);

        if (userIdStr == null) {
            return Optional.empty();
        }

        return Optional.of(UUID.fromString(userIdStr));
    }

    @Override
    public boolean delete(UUID userId) {
        if (userId == null) {
            return false;
        }

        String key = RedisKeyBuilder.refreshToken(userId);
        try {
            String storedHash = redisTemplate.opsForValue().get(key);
            if (storedHash != null) {
                String indexKey = RedisKeyBuilder.refreshTokenIndex(storedHash);
                redisTemplate.delete(indexKey);
            }

            Boolean result = Boolean.TRUE.equals(redisTemplate.delete(key));
            log.debug("RT 삭제 완료 — userId={}", userId);
            return result;
        } catch (DataAccessException e) {
            log.debug("RT 삭제 시도 — 키 없음 — userId={}", userId, e);
            return false;
        }
    }
}
