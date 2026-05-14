package com.pagely.authservice.infrastructure.client.dto;

import com.pagely.common.auth.Role;
import java.util.UUID;

/**
 * User Service 의 자격 검증, 권한 조회 API 응답 DTO.
 */
public record UserAuthInfoResponse(
        UUID userId,
        Role role
) {
}
