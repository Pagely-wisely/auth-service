package com.pagely.authservice.infrastructure.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagely.authservice.infrastructure.client.decoder.FeignApiResponseDecoder;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 디코더를 Bean으로 등록합니다. 전역 Feign Client에 설정합니다.
 *
 */
@Configuration
public class GlobalFeignClientConfig {

    @Bean
    public Decoder decoder(ObjectMapper objectMapper) {
        return new FeignApiResponseDecoder(objectMapper);
    }
}
