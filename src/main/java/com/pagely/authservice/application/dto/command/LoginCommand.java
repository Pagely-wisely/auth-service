package com.pagely.authservice.application.dto.command;

/**
 * 로그인 커맨드.
 */
public record LoginCommand(
        String loginId,
        String password
) {
}
