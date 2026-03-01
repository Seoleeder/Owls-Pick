package io.github.seoleeder.owls_pick.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Test Error
    TEST_ERROR(10000, HttpStatus.BAD_REQUEST, "테스트 에러입니다."),

    //400 Bad Request
    INVALID_REQUEST(40000, HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
    UNSUPPORTED_PROVIDER(40001, HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),
    INVALID_AUTHORIZATION_CODE(40002, HttpStatus.BAD_REQUEST, "인가 코드가 만료되었거나 유효하지 않습니다. "),

    // 401 Unauthorized
    UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED, "인증이 유효하지 않습니다"),
    ADMIN_KEY_ERROR(40101, HttpStatus.UNAUTHORIZED, "관리자 인증이 유효하지 않습니다"),
    EXPIRED_TOKEN(40102, HttpStatus.UNAUTHORIZED,"JWT 토큰이 만료되었습니다"),
    INVALID_TOKEN(40103, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    MALFORMED_TOKEN(40104, HttpStatus.UNAUTHORIZED, "잘못된 형식의 토큰입니다."),
    REVOKED_REFRESH_TOKEN(40105,HttpStatus.UNAUTHORIZED, "폐기되었거나 만료된 리프레시 토큰입니다."),
    INVALID_SIGNATURE(40106,HttpStatus.UNAUTHORIZED, "서명이 유효하지 않습니다."),


    // 404 Not Found
    NOT_FOUND_END_POINT(40400, HttpStatus.NOT_FOUND, "존재하지 않는 API입니다."),
    NOT_FOUND_USER(40401, HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    OAUTH_SERVER_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "소셜 로그인 서버와 통신 중 오류가 발생했습니다.");

    private final Integer code;
    private final HttpStatus httpStatus;
    private final String message;
}
