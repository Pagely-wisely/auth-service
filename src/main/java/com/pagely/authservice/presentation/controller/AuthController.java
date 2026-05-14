package com.pagely.authservice.presentation.controller;

import com.pagely.authservice.application.dto.command.LogoutCommand;
import com.pagely.authservice.application.dto.command.RefreshCommand;
import com.pagely.authservice.application.dto.result.AuthenticationResult;
import com.pagely.authservice.application.service.AuthApplicationService;
import com.pagely.authservice.infrastructure.util.CookieUtil;
import com.pagely.authservice.presentation.dto.request.LoginRequest;
import com.pagely.authservice.presentation.dto.response.AuthTokenResponse;
import com.pagely.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final CookieUtil cookieUtil;

    /**
     * 사용자 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-App-Platform", required = false) String platform,
            HttpServletResponse response) {

        AuthenticationResult result = authApplicationService.login(request.toCommand());

        // 1. 플랫폼에 따른 응답 본문 분기 (Web은 RT 제외, Mobile은 RT 포함)
        boolean isMobile = "mobile".equalsIgnoreCase(platform);
        AuthTokenResponse tokenResponse = isMobile
                ? AuthTokenResponse.forMobile(result)
                : AuthTokenResponse.forWeb(result);

        // 2. Refresh Token 쿠키 설정 (웹/모바일 공통 적용 정책 대응)
        ResponseCookie rtCookie = cookieUtil.createRefreshTokenCookie(result.getRefreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

        // ApiResponse.ok(Object data) 메서드 활용
        return ApiResponse.ok(tokenResponse);
    }

    /**
     * 토큰 갱신 (Refresh)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @RequestHeader(value = "X-App-Platform", required = false) String platform) {

        // 쿠키가 없는 경우에 대한 예외 처리는 필요에 따라 GlobalExceptionHandler나 Filter/Interceptor 단계에서 처리 가능
        AuthenticationResult result = authApplicationService.refresh(new RefreshCommand(refreshToken));

        boolean isMobile = "mobile".equalsIgnoreCase(platform);
        AuthTokenResponse tokenResponse = isMobile
                ? AuthTokenResponse.forMobile(result)
                : AuthTokenResponse.forWeb(result);

        return ApiResponse.ok(tokenResponse);
    }

    /**
     * 사용자 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authApplicationService.logout(new LogoutCommand(refreshToken));
        }

        // 쿠키 즉시 만료 처리
        ResponseCookie expiredCookie = cookieUtil.createExpiredRefreshTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());

        return ApiResponse.ok();
    }
}
