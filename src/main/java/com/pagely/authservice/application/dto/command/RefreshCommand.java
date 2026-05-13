package com.pagely.authservice.application.dto.command;

/**
 * 토큰 갱신 커맨드.
 */
public record RefreshCommand(
        String refreshToken
) {
}
