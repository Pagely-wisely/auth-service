package com.pagely.authservice.infrastructure.client.decoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagely.authservice.domain.exception.AuthErrorCode;
import com.pagely.common.exception.BusinessException;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserServiceClientErrorDecoder implements ErrorDecoder {
    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.body() == null) {
            return defaultErrorDecoder.decode(methodKey, response);
        }

        try (InputStream inputStream = response.body().asInputStream()) {
            JsonNode rootNode = objectMapper.readTree(inputStream);
            JsonNode errorNode = rootNode.get("error");

            if (errorNode != null) {
                String remoteErrorCode = errorNode.has("code") ? errorNode.get("code").asText() : "UNKNOWN_CODE";
                String remoteMessage = errorNode.has("message") ? errorNode.get("message").asText() : "Unknown Error";

                log.debug("UserService 통신 실패 - code: {}, message: {}", remoteErrorCode, remoteMessage);

                // 1. 자격 검증 실패 (401)
                if ("INVALID_CREDENTIALS".equals(remoteErrorCode) || "USER_NOT_FOUND".equals(remoteErrorCode)) {
                    return new BusinessException(AuthErrorCode.INVALID_CREDENTIALS, remoteMessage);
                }

                // 2. 정지된 유저 (403)
                if ("USER_SUSPENDED".equals(remoteErrorCode)) {
                    return new BusinessException(AuthErrorCode.USER_SUSPENDED, remoteMessage);
                }

                // 3. 그 외에 authservice가 굳이 세분화할 필요가 없는 유저 서비스의 기타 비즈니스 에러들
                return new BusinessException(AuthErrorCode.USER_SERVICE_BUSINESS_ERROR, remoteMessage);
            }

        } catch (IOException e) {
            log.error("FeignDecode (User Service Error) 파싱 실패", e);
        }
        // 파싱에 실패했거나 규격에 안 맞는 에러는 Feign 기본 예외로 처리
        return defaultErrorDecoder.decode(methodKey, response);
    }
}
