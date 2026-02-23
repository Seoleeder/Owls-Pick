package io.github.seoleeder.owls_pick.controller;

import io.github.seoleeder.owls_pick.client.oauth.factory.SocialAuthFactory;
import io.github.seoleeder.owls_pick.client.oauth.provider.SocialAuthProvider;
import io.github.seoleeder.owls_pick.controller.auth.AuthController;
import io.github.seoleeder.owls_pick.controller.auth.DevAuthController;
import io.github.seoleeder.owls_pick.dto.auth.LoginResponse;
import io.github.seoleeder.owls_pick.global.config.AdminAuthorizationInterceptor;
import io.github.seoleeder.owls_pick.global.config.properties.AdminProperties;
import io.github.seoleeder.owls_pick.global.security.config.properties.JwtProperties;
import io.github.seoleeder.owls_pick.global.security.jwt.JwtTokenProvider;
import io.github.seoleeder.owls_pick.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, DevAuthController.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private SocialAuthFactory socialAuthFactory;
    @MockitoBean private SocialAuthProvider socialAuthProvider;

    @MockitoBean private JwtProperties jwtProperties;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AdminProperties adminProperties;
    @MockitoBean private AdminAuthorizationInterceptor adminAuthorizationInterceptor;

    @Test
    @DisplayName("GET /login/{provider} - 소셜 로그인 URL 발급 확인")
    void getAuthCodeUrl_success() throws Exception {
        String expectedUrl = "https://kauth.kakao.com/oauth/authorize?client_id=test";
        given(socialAuthFactory.getProvider("kakao")).willReturn(socialAuthProvider);
        given(socialAuthProvider.getAuthCodeUrl()).willReturn(expectedUrl);

        mockMvc.perform(get("/api/auth/login/kakao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(expectedUrl));
    }

    @Test
    @DisplayName("POST /login/{provider} - 소셜 로그인 처리 및 토큰 발급 확인")
    void login_success() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .accessToken("at-token")
                .refreshToken("rt-token")
                .nickname("테스터")
                .build();
        given(authService.login(anyString(), anyString(), anyString())).willReturn(response);

        mockMvc.perform(post("/api/auth/login/kakao")
                        .param("code", "auth-code")
                        .param("state", "random-state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("at-token"))
                .andExpect(jsonPath("$.data.nickname").value("테스터"));
    }

    @Test
    @DisplayName("POST /reissue - 리프레시 토큰으로 새로운 토큰 재발급 확인")
    void reissue_success() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .accessToken("new-at")
                .refreshToken("old-rt")
                .build();
        given(authService.reissue(anyString())).willReturn(response);

        mockMvc.perform(post("/api/auth/reissue")
                        .header("Refresh-Token", "old-rt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-at"));
    }

    @Test
    @DisplayName("POST /logout - 로그아웃 처리 확인")
    void logout_success() throws Exception {
        // given
        String userId = "1";
        // 직접 Authentication 객체 생성 (username이 principal이 됨)
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(userId);
    }

    @Test
    @DisplayName("DELETE /withdraw - 회원 탈퇴 처리 확인")
    void withdraw_success() throws Exception {
        // given
        String userId = "1";
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

        // when & then
        mockMvc.perform(delete("/api/auth/withdraw")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).withdraw(userId);
    }

    @Test
    @DisplayName("POST /api/dev/auth/bypass - 개발용 백도어 로그인 확인")
    void bypassLogin_success() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .accessToken("dev-at")
                .email("test@kakao.com")
                .build();
        given(authService.bypassLogin("test@kakao.com")).willReturn(response);

        mockMvc.perform(post("/api/dev/auth/bypass")
                        .param("email", "test@kakao.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@kakao.com"));
    }
}