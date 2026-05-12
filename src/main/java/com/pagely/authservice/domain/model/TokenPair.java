package com.pagely.authservice.domain.model;

/** 로그인 시 발급되는 토큰 묶음*/
public record TokenPair(
    AccessToken accessToken,
    RefreshToken refreshToken
) {
}
