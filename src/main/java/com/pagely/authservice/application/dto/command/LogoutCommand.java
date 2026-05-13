package com.pagely.authservice.application.dto.command;

/**
 * 로그아웃 커맨드.
 */
public record LogoutCommand(
        String refreshToken
) {
}
