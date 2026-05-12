package com.pagely.authservice.domain.model;

import java.time.Instant;

public record RefreshToken (
        String value,
        Instant issuedAt,
        Instant expiresAt
) {
}
