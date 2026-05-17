package com.pagely.authservice.infrastructure.client.decoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagely.authservice.domain.exception.AuthErrorCode;
import com.pagely.common.exception.BusinessException;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserServiceClientErrorDecoder implements ErrorDecoder {
    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        // 1. 바디가 아예 없으면 상태 코드로 즉시 판단
        if (response.body() == null) {
            return handleByStatusCode(response);
        }

        try {
            String bodyString = feign.Util.toString(response.body().asReader(java.nio.charset.StandardCharsets.UTF_8));

            if (bodyString == null || bodyString.isBlank()) {
                return handleByStatusCode(response);
            }

            JsonNode rootNode = objectMapper.readTree(bodyString);
            JsonNode errorNode = rootNode.get("error");

            if (errorNode != null && !errorNode.isNull()) {
                String remoteErrorCode = errorNode.has("code") ? errorNode.get("code").asText() : "UNKNOWN_CODE";
                String remoteMessage = errorNode.has("message") ? errorNode.get("message").asText() : "Unknown Error";

                log.debug("UserService 통신 실패 파싱 성공 - code: {}, message: {}", remoteErrorCode, remoteMessage);

                if ("INVALID_CREDENTIALS".equals(remoteErrorCode) || "USER_NOT_FOUND".equals(remoteErrorCode)) {
                    return new BusinessException(AuthErrorCode.INVALID_CREDENTIALS, remoteMessage);
                }
                if ("USER_SUSPENDED".equals(remoteErrorCode)) {
                    return new BusinessException(AuthErrorCode.USER_SUSPENDED, remoteMessage);
                }
                return new BusinessException(AuthErrorCode.USER_SERVICE_BUSINESS_ERROR, remoteMessage);
            }

        } catch (IOException e) {
            log.error("FeignDecode (User Service Error) 바디 읽기 또는 파싱 실패", e);
        }

        // 파싱에 실패했더라도 401, 403이면 기본 디코더로 넘기지 않고 여기서 잡기 (2차 방어선)
        return handleByStatusCode(response);
    }

    private Exception handleByStatusCode(Response response) {
        if (response.status() == 401) {
            return new BusinessException(AuthErrorCode.INVALID_CREDENTIALS, "인증 자격 증명이 유효하지 않습니다.");
        }
        if (response.status() == 403) {
            return new BusinessException(AuthErrorCode.USER_SUSPENDED, "정지된 사용자입니다.");
        }
        return defaultErrorDecoder.decode(response.reason(), response);
    }
}
