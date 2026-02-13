package io.github.seoleeder.owls_pick.common.response;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 존재하지 않는 요청에 대한 예외
    @ExceptionHandler(value = {NoHandlerFoundException.class, HttpRequestMethodNotSupportedException.class})
    public CommonResponse<?> handleNoPageFoundException(Exception e) {
        log.error("GlobalExceptionHandler catch NoHandlerFoundException : {}", e.getMessage());
        return CommonResponse.fail(ErrorCode.NOT_FOUND_END_POINT);
    }


    // 커스텀 예외
    @ExceptionHandler(value = {CustomException.class})
    public CommonResponse<?> handleCustomException(CustomException e) {
        log.error("handleCustomException() in GlobalExceptionHandler throw CustomException : {}", e.getMessage());
        return CommonResponse.fail(e.getErrorCode());
    }

    // 기본 예외
    @ExceptionHandler(value = {Exception.class})
    public CommonResponse<?> handleException(Exception e) {
        log.error("handleException() in GlobalExceptionHandler throw Exception : {}", e.getMessage());
        e.printStackTrace();
        return CommonResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
