package io.github.seoleeder.owls_pick.controller.auth;

import io.github.seoleeder.owls_pick.dto.auth.LoginResponse;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/auth")
@RequiredArgsConstructor
@Tag(name = "[DEV] 소셜 로그인 API", description = "운영(prod) 환경에서는 비활성화되는 개발용 백도어 API입니다.")
public class DevAuthController {

    private final AuthService authService;

    @Operation(
            summary = "개발용 백도어 로그인",
            description = "소셜 인증 과정을 생략하고, 원하는 이메일로 즉시 로그인(가입)하여 JWT 토큰 발급"
    )
    @PostMapping("/bypass")
    public CommonResponse<LoginResponse> bypassLogin(
            // 이메일을 안 보내면 기본값으로 test@kakao.com 삽입
            @RequestParam(defaultValue = "test@kakao.com") String email) {

        LoginResponse response = authService.bypassLogin(email);
        return CommonResponse.ok(response);
    }
}
