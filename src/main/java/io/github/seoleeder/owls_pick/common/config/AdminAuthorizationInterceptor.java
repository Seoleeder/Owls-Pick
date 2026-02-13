package io.github.seoleeder.owls_pick.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seoleeder.owls_pick.common.config.properties.AdminProperties;
import io.github.seoleeder.owls_pick.common.response.CommonResponse;
import io.github.seoleeder.owls_pick.common.response.CustomException;
import io.github.seoleeder.owls_pick.common.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {

    private final AdminProperties props;

    public AdminAuthorizationInterceptor(AdminProperties props){
        this.props = props;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 요청 헤더(X-ADMIN-KEY)에서 키 꺼내기
        String adminKey = request.getHeader("X-ADMIN-KEY");

        // 키 불일치 예외 던지기
        if (!props.adminKey().equals(adminKey)) {
            log.warn("[Admin Security] Invalid Key Access! IP: {}", request.getRemoteAddr());
            throw new CustomException(ErrorCode.ADMIN_KEY_ERROR);
        }

        return true;

    }
}
