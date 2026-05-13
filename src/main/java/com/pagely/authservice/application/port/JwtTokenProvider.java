package com.pagely.authservice.application.port;

import com.pagely.authservice.domain.model.AccessToken;
import com.pagely.authservice.domain.model.RefreshToken;
import com.pagely.authservice.domain.model.TokenPayload;
import com.pagely.common.auth.Role;
import java.util.UUID;

/**
 * 토큰 발급, 검증
 */
public interface JwtTokenProvider {
    AccessToken createAccessToken(UUID userID, Role role);

    RefreshToken createRefreshToken();

    TokenPayload validateAndExtract(String accessToken);
}

