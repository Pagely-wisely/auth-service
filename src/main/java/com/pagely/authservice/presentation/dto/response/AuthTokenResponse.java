package com.pagely.authservice.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pagely.authservice.application.dto.result.AuthenticationResult;
import java.time.Instant;

/**
 * 인증 토큰 응답 (OAuth 2.0 RFC 6749 형식).
 *
 * <p>refreshToken 은 모바일에만 포함 (@JsonInclude).</p>
 */
public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String refreshToken
) {

    /**
     * 웹 응답 (RT 미포함, 쿠키 사용).
     */
    public static AuthTokenResponse forWeb(AuthenticationResult result) {
        long expiresIn = calculateExpiresInSeconds(result.tokenPair().accessToken().expiresAt());

        return new AuthTokenResponse(
                result.getAccessToken(),
                "Bearer",
                expiresIn,
                null  // 웹은 쿠키에서 RT 관리
        );
    }

    /**
     * 모바일 응답 (RT 포함).
     */
    public static AuthTokenResponse forMobile(AuthenticationResult result) {
        long expiresIn = calculateExpiresInSeconds(result.tokenPair().accessToken().expiresAt());

        return new AuthTokenResponse(
                result.getAccessToken(),
                "Bearer",
                expiresIn,
                result.getRefreshToken()  // 모바일은 본문에 RT
        );
    }

    /**
     * AT 만료까지 남은 시간 (초).
     */
    private static long calculateExpiresInSeconds(Instant expiresAt) {
        return Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
    }
}
