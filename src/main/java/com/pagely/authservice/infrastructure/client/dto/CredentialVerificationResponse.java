package com.pagely.authservice.infrastructure.client.dto;

import com.pagely.common.auth.Role;
import java.util.UUID;

/**
 * User Service 의 자격 검증 API 응답 DTO.ß
 */
public record CredentialVerificationResponse(
        UUID userId,
        Role role
) {
}
