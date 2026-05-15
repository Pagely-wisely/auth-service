package com.pagely.authservice.application.port;

import com.pagely.common.auth.Role;
import java.util.UUID;

/**
 * 사용자 자격 검증 책임의 추상화
 */
public interface UserCredentialProvider {

    /**
     * 자격 검증 결과.
     *
     * @param userId 검증된 사용자 ID
     * @param role   사용자 권한
     */
    record Result(UUID userId, Role role) {
    }

    /**
     * 자격 검증.
     */
    Result verify(String loginId, String password);

    /**
     * 자격 조회
     *
     * @param userId
     */
    Result findIdentity(UUID userId);
}
