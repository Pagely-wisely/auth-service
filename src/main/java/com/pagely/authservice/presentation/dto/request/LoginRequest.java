package com.pagely.authservice.presentation.dto.request;

import com.pagely.authservice.application.dto.command.LoginCommand;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "loginId 는 필수입니다.") String loginId,
        @NotBlank(message = "password 는 필수입니다.") String password
) {

    public LoginCommand toCommand() {
        return new LoginCommand(loginId, password);
    }
}
