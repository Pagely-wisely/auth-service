package com.pagely.authservice.infrastructure.client;

import com.pagely.authservice.infrastructure.client.config.UserServiceFeignConfig;
import com.pagely.authservice.infrastructure.client.dto.CredentialVerificationRequest;
import com.pagely.authservice.infrastructure.client.dto.UserAuthInfoResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * User Service 의 내부 API 호출용 Feign 클라이언트.
 */
@FeignClient(name = "userservice",
        configuration = UserServiceFeignConfig.class) // ErrorDecoder 설정
public interface UserServiceClient {

    // 로그인용
    @PostMapping("/internal/users/credential-verifications")
    UserAuthInfoResponse verifyCredentials(@RequestBody CredentialVerificationRequest request);

    // 리프레시 토큰/권한 확인용
    @GetMapping("/internal/users/{userId}/auth-info")
    UserAuthInfoResponse getAuthInfo(@PathVariable("userId") UUID userId);
}
