package com.pagely.authservice.application.dto.result;

import com.pagely.authservice.domain.model.TokenPair;
import java.util.UUID;

/**
 * 인증 결과.
 *
 * <p>Controller 에서 응답 DTO 로 변환.</p>
 */
public record AuthenticationResult(
        UUID userId,
        TokenPair tokenPair
) {

    public static AuthenticationResult of(
            UUID userId,
            TokenPair tokenPair
    ) {
        return new AuthenticationResult(userId, tokenPair);
    }

    /**
     * AT 값 추출.
     */
    public String getAccessToken() {
        return tokenPair.accessToken().value();
    }

    /**
     * RT 값 추출.
     */
    public String getRefreshToken() {
        return tokenPair.refreshToken().value();
    }
}
