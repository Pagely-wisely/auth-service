// pagely-auth-service/src/main/java/com/pagely/authservice/domain/exception/AuthErrorCode.java

package com.pagely.authservice.domain.exception;

import com.pagely.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_ACCESS_TOKEN("유효하지 않은 액세스 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_ACCESS_TOKEN("만료된 액세스 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_REFRESH_TOKEN("만료된 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("갱신 토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("로그인 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    USER_SUSPENDED("계정이 정지된 상태입니다.", HttpStatus.FORBIDDEN),
    USER_SERVICE_UNAVAILABLE("사용자 서비스에 연결할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    USER_SERVICE_BUSINESS_ERROR("사용자 서비스 처리 중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST);
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    AuthErrorCode(String message, HttpStatus httpStatus) {
        this.code = this.name();
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
