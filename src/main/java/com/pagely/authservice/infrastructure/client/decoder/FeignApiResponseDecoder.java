package com.pagely.authservice.infrastructure.client.decoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagely.common.exception.BusinessException;
import com.pagely.common.exception.CommonErrorCode;
import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import java.io.IOException;
import java.lang.reflect.Type;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FeignApiResponseDecoder implements Decoder {
    private final ObjectMapper objectMapper;

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {

        // 1. 응답 바디가 없으면 null 반환
        if (response.body() == null) {
            return null;
        }

        // 2. ApiResponse 객체로 바로 읽지 않고, 일단 트리 구조(JsonNode)로 읽어옵니다.
        JsonNode rootNode = objectMapper.readTree(response.body().asInputStream());

        // 3. ApiResponse 껍데기 필드값 추출
        boolean success = rootNode.has("success") && rootNode.get("success").asBoolean();

        // 4. HTTP 2xx로 왔는데 success가 false인 비정상적인 상황 방어
        if (!success) {
            log.error("Feign 2xx Response but success is false");
            // Decoder에서는 return이 아니라 throw를 해야 예외가 터집니다.
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "API 응답이 성공(2xx)이나 success가 false입니다.");
        }

        // 5. data 필드가 null 이거나 없으면 null 반환
        JsonNode dataNode = rootNode.get("data");
        if (dataNode == null || dataNode.isNull()) {
            return null;
        }

        // 6. 실제 원하는 타입(T)으로 변환
        return objectMapper.convertValue(dataNode, objectMapper.getTypeFactory().constructType(type));
    }
}
