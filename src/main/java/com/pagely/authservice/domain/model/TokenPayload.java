package com.pagely.authservice.domain.model;

import com.pagely.common.auth.Role;
import java.time.Instant;
import java.util.UUID;

/** AccessToken 검증 후 추출된 사용자 정보*/
public record TokenPayload(
        UUID userId,
        Role role,
        String jti,
        Instant issuedAt,
        Instant expiresAt
) {
}
