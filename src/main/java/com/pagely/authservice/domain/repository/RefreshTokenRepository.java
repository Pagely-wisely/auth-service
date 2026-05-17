package com.pagely.authservice.domain.repository;

import com.pagely.authservice.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    /**
     * RT 저장. 같은 userId 의 기존 RT 가 있으면 덮어씀.
     *
     * @param userId       대상 사용자
     * @param refreshToken 발급된 RT (평문)
     */
    void save(UUID userId, RefreshToken refreshToken);

    /**
     * RT 검증.
     *
     * <p>전달된 RT의 해시로 저장된 userId가 있는지 확인 </p>
     */
    Optional<UUID> findUserIdByToken(String rawToken);

    /**
     * 사용자의 RT 삭제 (로그아웃 / 강제 무효화).
     *
     * @return 삭제 성공 여부
     */
    boolean delete(UUID userId);
}
