package com.pagely.authservice.infrastructure.client.dto;

/**
 * User Service 의 자격 검증 API 호출용 Request DTO.
 *
 * <p>User Service 의 presentation Request 와 형식 일치.
 * 팀 컨벤션 (각 서비스 자체 Feign) 에 따라 자체 정의.</p>
 */
public record CredentialVerificationRequest(
        String loginId,
        String password
) {
}
