package com.pagely.authservice.infrastructure.util;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final String COOKIE_NAME = "refreshToken";
    private static final long MAX_AGE_DAYS = 7;

    private final boolean isSecure;

    // dev가 아니면 isSecure true(운영)로 설정
    public CookieUtil(@Value("${spring.profiles.active:dev}") String activeProfile) {
        this.isSecure = !"dev".equalsIgnoreCase(activeProfile);
    }

    /**
     * 유효한 Refresh Token 쿠키 생성
     */
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)                // XSS 공격 방지 (자바스크립트 접근 불가)
                .secure(isSecure)              // dev 환경에선 false, 그 외(prod)엔 true (HTTPS 필수)
                .sameSite("Strict")            // CSRF 공격 완전히 방지
                .path("/")                     // 전역 경로에서 쿠키 전송 가능
                .maxAge(Duration.ofDays(MAX_AGE_DAYS)) // 7일 유지
                .build();
    }

    /**
     * 로그아웃 시 사용할 만료된 쿠키 생성
     */
    public ResponseCookie createExpiredRefreshTokenCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)                     // 즉시 만료시켜 브라우저에서 삭제 유도
                .build();
    }
}
