package com.pagely.authservice.infrastructure.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String issuer,
        AccessToken accessToken,
        RefreshToken refreshToken
) {
    public record AccessToken(int expirationMinutes) {
        public Duration toDuration() {
            return Duration.ofMinutes(expirationMinutes);
        }
    }

    public record RefreshToken(int expirationDays) {
        public Duration toDuration() {
            return Duration.ofDays(expirationDays);
        }
    }
}
