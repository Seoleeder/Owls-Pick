package io.github.seoleeder.owls_pick.service;

import io.github.seoleeder.owls_pick.client.oauth.factory.SocialAuthFactory;
import io.github.seoleeder.owls_pick.client.oauth.provider.SocialAuthProvider;
import io.github.seoleeder.owls_pick.dto.auth.LoginResponse;
import io.github.seoleeder.owls_pick.dto.auth.SocialTokenDto;
import io.github.seoleeder.owls_pick.dto.auth.SocialUserResponse;
import io.github.seoleeder.owls_pick.entity.user.SocialAccount;
import io.github.seoleeder.owls_pick.entity.user.User;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.global.security.config.properties.JwtProperties;
import io.github.seoleeder.owls_pick.global.security.jwt.JwtTokenProvider;
import io.github.seoleeder.owls_pick.repository.SocialAccountRepository;
import io.github.seoleeder.owls_pick.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private SocialAuthFactory socialAuthFactory;
    @Mock private UserRepository userRepository;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations; // Redis 값 조작 Mock
    @Mock private JwtProperties jwtProperties;
    @Mock private SocialAuthProvider socialAuthProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // redisTemplate.opsForValue()가 호출될 때 mock 객체를 반환하도록 설정
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("소셜 로그인 성공 - 기존 회원이 로그인하는 경우")
    void login_existing_user_success() {
        // given
        String providerName = "kakao";
        String code = "test-code";
        String state = "test-state";

        SocialTokenDto tokenDto = new SocialTokenDto("access-token", "refresh-token");
        SocialUserResponse userInfo = new SocialUserResponse("12345", "test@kakao.com", "한지수");
        User user = User.builder().id(1L).email("test@kakao.com").name("한지수").build();
        SocialAccount socialAccount = SocialAccount.builder().user(user).build();

        given(socialAuthFactory.getProvider(anyString())).willReturn(socialAuthProvider);
        given(socialAuthProvider.fetchAccessToken(anyString(), anyString())).willReturn(tokenDto);
        given(socialAuthProvider.getUserInfo(any())).willReturn(userInfo);

        given(socialAccountRepository.findByProviderAndProviderId(any(), anyString()))
                .willReturn(Optional.of(socialAccount));

        given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("new-refresh-token");
        given(jwtProperties.refreshTokenValidity()).willReturn(3600000L);

        // when
        LoginResponse response = authService.login(providerName, code, state);

        // then
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.email()).isEqualTo("test@kakao.com");

        // Redis 저장 로직이 실행되었는지 검증
        verify(valueOperations, times(1)).set(
                eq("RT:1"), eq("new-refresh-token"), anyLong(), eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("로그아웃 성공 - Redis에서 토큰 삭제")
    void logout_success() {
        // given
        String userId = "1";
        String redisKey = "RT:" + userId;
        given(redisTemplate.hasKey(redisKey)).willReturn(true);

        // when
        authService.logout(userId);

        // then
        verify(redisTemplate, times(1)).delete(redisKey);
    }

    @Test
    @DisplayName("토큰 재발급 성공 - 유효한 RT가 전달되면 신규 AT 반환")
    void reissue_success() {
        // given
        String refreshToken = "valid-refresh-token";
        String userId = "1";
        User user = User.builder().id(1L).email("test@kakao.com").name("한지수").build();

        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(valueOperations.get("RT:" + userId)).willReturn(refreshToken); // Redis에 저장된 토큰과 일치
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn("new-access-token");

        // when
        LoginResponse response = authService.reissue(refreshToken);

        // then
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        verify(jwtTokenProvider).validateToken(refreshToken);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 저장된 토큰과 일치하지 않음")
    void reissue_fail_token_mismatch() {
        // given
        String refreshToken = "stolen-refresh-token";
        String userId = "1";
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(valueOperations.get("RT:" + userId)).willReturn("original-refresh-token"); // 불일치 발생

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - Redis 토큰 삭제 및 DB 정보 삭제 확인")
    void withdraw_success() {
        // given
        String userId = "1";
        String redisKey = "RT:" + userId;
        given(redisTemplate.hasKey(redisKey)).willReturn(true);

        // when
        authService.withdraw(userId);

        // then
        verify(redisTemplate).delete(redisKey); // 로그아웃(토큰삭제) 호출 확인
        verify(socialAccountRepository).deleteByUserId(1L); // 소셜 연동 삭제 확인
        verify(userRepository).deleteById(1L); // 유저 삭제 확인
    }

    @Test
    @DisplayName("백도어 로그인 성공 - 소셜 연동 없이 이메일로 즉시 로그인")
    void bypassLogin_success() {
        // given
        String email = "backdoor@kakao.com";
        User user = User.builder().id(999L).email(email).name("테스트유저_backdoor").build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn("bt-at");
        given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("bt-rt");
        given(jwtProperties.refreshTokenValidity()).willReturn(3600000L);

        // when
        LoginResponse response = authService.bypassLogin(email);

        // then
        assertThat(response.nickname()).contains("테스트유저");
        verify(valueOperations).set(eq("RT:999"), eq("bt-rt"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }
}
