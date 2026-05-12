package com.pagely.authservice.domain.model;

import java.time.Instant;
/**
 * 발급된 Refresh Token 의 값 객체.
 *
 * <p>Opaque String + 메타 정보. Redis 에 해시로 저장 / HttpOnly 쿠키 응답.</p>
 *
 * <p>JWT 와 달리 의미 없는 랜덤 문자열. 해킹 시 정보 노출 없음.</p>
 */
public record RefreshToken (
        String value,
        Instant issuedAt,
        Instant expiresAt
) {
}
