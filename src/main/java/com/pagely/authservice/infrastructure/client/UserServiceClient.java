package com.pagely.authservice.infrastructure.client;

import com.pagely.authservice.infrastructure.client.dto.CredentialVerificationRequest;
import com.pagely.authservice.infrastructure.client.dto.CredentialVerificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * User Service 의 내부 API 호출용 Feign 클라이언트.
 */
@FeignClient(name = "userservice")
public interface UserServiceClient {

    @PostMapping("/internal/users/credential-verifications")
    CredentialVerificationResponse verifyCredentials(@RequestBody CredentialVerificationRequest request);
}
