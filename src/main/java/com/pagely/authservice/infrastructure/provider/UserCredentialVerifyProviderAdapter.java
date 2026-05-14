package com.pagely.authservice.infrastructure.provider;

import com.pagely.authservice.application.port.UserCredentialProvider;
import com.pagely.authservice.domain.exception.AuthErrorCode;
import com.pagely.authservice.infrastructure.client.UserServiceClient;
import com.pagely.authservice.infrastructure.client.dto.CredentialVerificationRequest;
import com.pagely.authservice.infrastructure.client.dto.UserAuthInfoResponse;
import com.pagely.common.exception.BusinessException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * User Service Feign 호출 기반 자격 검증 어댑터.
 *
 * <p><b>에러 처리</b></p>
 * <ul>
 *   <li>401 → LOGIN_FAILED (자격 불일치)</li>
 *   <li>기타 (5xx / 네트워크) → USER_SERVICE_UNAVAILABLE</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCredentialVerifyProviderAdapter implements UserCredentialProvider {

    private final UserServiceClient userServiceClient;

    @Override
    public Result verify(String loginId, String password) {
        try {
            UserAuthInfoResponse response = userServiceClient.verifyCredentials(
                    new CredentialVerificationRequest(loginId, password)
            );
            return new Result(response.userId(), response.role());

        } catch (FeignException.Forbidden e) {
            log.debug("User isSuspended true  — code={}", loginId);
            throw new BusinessException(AuthErrorCode.USER_SUSPENDED);
        } catch (FeignException.Unauthorized e) {
            log.debug("User Service 자격 검증 실패 — loginId={}", loginId);
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);

        } catch (FeignException e) {
            log.error("User Service 통신 오류 — status={}, message={}", e.status(), e.getMessage());
            throw new BusinessException(AuthErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }
}
