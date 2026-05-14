package com.pagely.authservice.presentation.dto.request;

import com.pagely.authservice.application.dto.command.RefreshCommand;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken 은 필수입니다.") String refreshToken
) {
    public RefreshCommand toCommand() {
        return new RefreshCommand(refreshToken);
    }
}
