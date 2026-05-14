package com.pagely.authservice.presentation.dto.request;

import com.pagely.authservice.application.dto.command.LogoutCommand;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank(message = "refreshToken 은 필수입니다.") String refreshToken
) {
    public LogoutCommand toCommand() {
        return new LogoutCommand(refreshToken);
    }
}
