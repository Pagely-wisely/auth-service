package com.pagely.authservice.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * RT 해시 유틸.
 *
 * <p>SHA-256 + Base64URL. 평문 → 고정 길이 해시.</p>
 */
public final class TokenHasher {

    private static final String ALGORITHM = "SHA-256";

    private TokenHasher() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("rawToken 은 비어 있을 수 없습니다.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 JVM 표준 — 발생 시 환경 자체 문제
            throw new IllegalStateException("SHA-256 미지원 환경", e);
        }
    }
}
