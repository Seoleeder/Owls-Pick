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
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
@Tag(name = "[ADMIN] 관리자 인증 API", description = "X-ADMIN-KEY 헤더 인증 기반 관리자 전용 회원 관리 및 토큰 발급 API")
public class AdminAuthController {

    private final AuthService authService;

    @Operation(
            summary = "관리자 바이패스 로그인",
            description = "소셜 인가 과정을 생략하고 이메일 기반으로 회원가입/로그인 및 JWT 토큰 발급"
    )
    @PostMapping("/bypass/login")
    public CommonResponse<LoginResponse> bypassLogin(
            @RequestParam(defaultValue = "admin@kakao.com") String email) {

        LoginResponse response = authService.bypassLogin(email);
        return CommonResponse.ok(response);
    }

    @Operation(
            summary = "관리자 바이패스 로그아웃",
            description = "지정한 이메일의  Refresh Token을 삭제하여 로그아웃 처리"
    )
    @PostMapping("/bypass/logout")
    public CommonResponse<Void> bypassLogout(
            @RequestParam String email) {

        authService.bypassLogout(email);
        return CommonResponse.ok(null);
    }
}
