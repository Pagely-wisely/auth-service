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
    EXPIRED_ACCESS_TOKEN( "만료된 액세스 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN( "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_REFRESH_TOKEN( "만료된 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    LOGIN_FAILED( "로그인에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    USER_SERVICE_UNAVAILABLE("사용자 서비스에 연결할 수 없습니다.",HttpStatus.SERVICE_UNAVAILABLE),
    ;
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    AuthErrorCode(String message, HttpStatus httpStatus) {
        this.code = this.name();
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
