package com.pagely.authservice.infrastructure.persistence;

import com.pagely.authservice.domain.model.RefreshToken;
import com.pagely.authservice.domain.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis 기반 RT 화이트리스트 저장소.
 *
 * <p><b>저장 구조</b></p>
 * <ul>
 *   <li>Key: {@code auth-service:rt:{userId}}</li>
 *   <li>Value: 토큰 해시 (SHA-256 + Base64URL, String)</li>
 *   <li>TTL: RT 만료까지 (자동 정리)</li>
 * </ul>
 *
 * <p><b>설계 의도</b></p>
 * <ul>
 *   <li>도메인 모델 (RefreshToken) 과 저장 구조 분리</li>
 *   <li>검증에 필요한 최소 정보 (해시) 만 저장</li>
 *   <li>발급 / 만료 시각은 Redis TTL 이 자동 관리</li>
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

        redisTemplate.opsForValue().set(key, tokenHash, Duration.ofSeconds(ttlSeconds));
        log.debug("RT 저장 완료 — userId={}, ttl={}s", userId, ttlSeconds);
    }

    @Override
    public boolean validate(UUID userId, String rawToken) {
        if (userId == null || rawToken == null || rawToken.isBlank()) {
            return false;
        }

        String key = RedisKeyBuilder.refreshToken(userId);
        String storedHash = redisTemplate.opsForValue().get(key);

        if (storedHash == null) {
            log.debug("RT 미발견 — userId={}", userId);
            return false;
        }

        String inputHash = TokenHasher.hash(rawToken);
        if (!storedHash.equals(inputHash)) {
            log.warn("RT 해시 불일치 — userId={}", userId);
            return false;
        }

        // 만료 정보는 Redis TTL 이 관리 → 키 존재 = 유효
        return true;
    }

    @Override
    public boolean delete(UUID userId) {
        if (userId == null) {
            return false;
        }

        String key = RedisKeyBuilder.refreshToken(userId);
        boolean result = Boolean.TRUE.equals(redisTemplate.delete(key));

        if (result) {
            log.debug("RT 삭제 완료 — userId={}", userId);
        } else {
            log.debug("RT 삭제 시도 — 키 없음 — userId={}", userId);
        }

        return result;
    }
}
