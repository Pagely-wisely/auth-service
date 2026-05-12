package com.pagely.authservice.infrastructure.persistence;

import java.util.UUID;

/**
 * Auth Service 의 Redis 키 네임스페이스.
 *
 * <p>공유 Redis 인스턴스에서 키 충돌 방지 + 디버깅</p>
 * <p>규칙: {service-name}:{domain}:{id}</p>
 */
public final class RedisKeyBuilder {

    private static final String SERVICE_PREFIX = "auth-service";

    private RedisKeyBuilder() {
        throw new UnsupportedOperationException("Utility class");
    }
 
    /**
     * RT 저장 키.
     *
     * <p>예: {@code auth-service:rt:550e8400-e29b-41d4-a716-446655440000}</p>
     */
    public static String refreshToken(UUID userId) {
        return SERVICE_PREFIX + ":rt:" + userId;
    }

    /**
     * AT 블랙리스트 키
     *
     * <p>예: {@code auth-service:bl:jti:abc-123}</p>
     */
    public static String accessTokenBlacklist(String jti) {
        return SERVICE_PREFIX + ":bl:jti:" + jti;
    }
}
