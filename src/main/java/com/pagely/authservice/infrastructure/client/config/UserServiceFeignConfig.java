package com.pagely.authservice.infrastructure.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

/**
 * UserServiceClient 전용 설정
 */
public class UserServiceFeignConfig {

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new com.pagely.authservice.infrastructure.client.decoder.UserServiceClientErrorDecoder(objectMapper);
    }
}
