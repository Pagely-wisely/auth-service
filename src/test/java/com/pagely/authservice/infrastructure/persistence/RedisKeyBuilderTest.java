package com.pagely.authservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RedisKeyBuilderTest {
    @Test
    @DisplayName("RT 키는 service prefix + userId")
    void refreshTokenKey() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        String key = RedisKeyBuilder.refreshToken(userId);

        assertThat(key).isEqualTo("auth-service:rt:550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    @DisplayName("AT 블랙리스트 키")
    void blacklistKey() {
        String key = RedisKeyBuilder.accessTokenBlacklist("abc-jti");

        assertThat(key).isEqualTo("auth-service:bl:jti:abc-jti");
    }
}
