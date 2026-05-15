package com.pagely.authservice.application.service;

import com.pagely.authservice.application.dto.command.LoginCommand;
import com.pagely.authservice.application.dto.command.LogoutCommand;
import com.pagely.authservice.application.dto.command.RefreshCommand;
import com.pagely.authservice.application.dto.result.AuthenticationResult;
import com.pagely.authservice.application.port.JwtTokenProvider;
import com.pagely.authservice.application.port.UserCredentialProvider;
import com.pagely.authservice.domain.exception.AuthErrorCode;
import com.pagely.authservice.domain.model.AccessToken;
import com.pagely.authservice.domain.model.RefreshToken;
import com.pagely.authservice.domain.model.TokenPair;
import com.pagely.authservice.domain.repository.RefreshTokenRepository;
import com.pagely.common.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final UserCredentialProvider credentialProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    /*
     * <p>로그인</p>
     *
     * <ol>
     * <li>User Service 에서 자격 검증 (Feign)</li>
     * <li>AccessToken(JWT) 및 RefreshToken(Opaque) 생성</li>
     * <li>RefreshToken 을 Repository(Redis) 에 저장</li>
     * </ol>
     */

    @Transactional
    public AuthenticationResult login(LoginCommand command) {
        // 1. 자격 검증 및 초기 권한 획득
        UserCredentialProvider.Result identity = credentialProvider.verify(
                command.loginId(),
                command.password()
        );

        // 2. 토큰 생성 (AT에는 Role 포함, RT는 랜덤 문자열)
        AccessToken accessToken = jwtTokenProvider.createAccessToken(identity.userId(), identity.role());
        RefreshToken refreshToken = jwtTokenProvider.createRefreshToken();

        // 3. Redis 저장 (userId와 RT 해시값 매핑)
        refreshTokenRepository.save(identity.userId(), refreshToken);

        log.info("로그인 완료 — userId={}", identity.userId());

        return AuthenticationResult.of(identity.userId(), new TokenPair(accessToken, refreshToken));
    }

    /*
     * <p>토큰 갱신</p>
     *
     * <ol>
     * <li>전달된 RefreshToken 으로 저장소에서 userId 조회</li>
     * <li>User Service 에서 해당 유저의 최신 권한(Role) 조회 (Feign)</li>
     * <li>새로운 AccessToken 발급 (기존 RT 유지)</li>
     * </ol>
     */

    @Transactional
    public AuthenticationResult refresh(RefreshCommand command) {
        // 1. Redis 에서 토큰 주인 식별 (RT 검증 포함)
        UUID userId = refreshTokenRepository.findUserIdByToken(command.refreshToken())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        // 2. 실시간 권한 조회 (최신 데이터 보장)
        UserCredentialProvider.Result identity = credentialProvider.findIdentity(userId);
        // 3. 새 AT 발급 및 응답
        AccessToken newAccessToken = jwtTokenProvider.createAccessToken(userId, identity.role());
        RefreshToken currentRefreshToken = new RefreshToken(command.refreshToken(), null, null);

        log.info("토큰 갱신 완료 — userId={}", userId);

        return AuthenticationResult.of(userId, new TokenPair(newAccessToken, currentRefreshToken));
    }

    /*
     * <p>로그아웃</p>
     *
     * <ol>
     * <li>전달된 RefreshToken 을 통해 유저 식별</li>
     * <li>저장소(Redis)에서 해당 유저의 RefreshToken 삭제</li>
     * </ol>
     */

    @Transactional
    public void logout(LogoutCommand command) {
        UUID userId = refreshTokenRepository.findUserIdByToken(command.refreshToken())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        refreshTokenRepository.delete(userId);

        log.info("로그아웃 완료 — userId={}", userId);
    }
}
