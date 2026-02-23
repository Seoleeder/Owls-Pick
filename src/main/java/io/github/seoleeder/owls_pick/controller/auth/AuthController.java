package io.github.seoleeder.owls_pick.controller.auth;

import io.github.seoleeder.owls_pick.client.oauth.factory.SocialAuthFactory;
import io.github.seoleeder.owls_pick.dto.auth.LoginResponse;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated // 파라미터 유효성 검사 활성화
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "소셜 인증/인가 API", description = "OAuth 2.0 소셜 로그인, 토큰 재발급, 로그아웃, 회원 탈퇴 처리")
public class AuthController {

    private final AuthService authService;
    private final SocialAuthFactory socialAuthFactory;

    /**
     * 소셜 로그인 페이지 URL 발급 (프론트엔드 리다이렉트용)
     */
    @Operation(
            summary = "소셜 로그인 페이지 URL 발급",
            description = "프론트엔드에서 사용자를 리다이렉트시킬 카카오/구글 등의 로그인 페이지 주소를 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "URL 발급 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = """
                                {
                                    "success": true,
                                    "data": "https://kauth.kakao.com/oauth/authorize?client_id=...",
                                    "error": null
                                }
                                """))),
                    @ApiResponse(responseCode = "400", description = "지원하지 않는 제공자 요청", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                                    { 
                                    "success": false, 
                                    "data": null, 
                                    "error": {  "code": 40001,
                                                "message": "지원하지 않는 소셜 로그인 제공자입니다." 
                                            } 
                                }
                            """)))
            }
    )
    @GetMapping("/login/{provider}")
    public CommonResponse<String> getAuthCodeUrl(
            @Parameter(description = "소셜 제공자 (kakao, google, naver)", required = true)
            @PathVariable @NotBlank String provider) {

        String authCodeUrl = socialAuthFactory.getProvider(provider).getAuthCodeUrl();
        return CommonResponse.ok(authCodeUrl);
    }

    @Operation(
            summary = "소셜 로그인 처리 및 토큰 발급",
            description = "소셜 로그인 후, Owl's Pick의 Access Token과 Refresh Token 발급",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공 및 토큰 발급 완료", content = @Content(schema = @Schema(example = """
                            {
                                "success": true,
                                "data": {
                                    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
                                    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
                                    "nickname": "한지수",
                                    "email": "jisuhan00@gmail.com"
                                },
                                "error": null
                            }
                            """))),
                    @ApiResponse(responseCode = "400", description = "필수 파라미터 누락 (code 없음, 네이버 state 누락 등)", content = @Content(schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "40000",
                                    "message": "잘못된 요청입니다."
                                }
                            }
                            """))),
                    @ApiResponse(responseCode = "400", description = "지원하지 않는 소셜 제공자 (예: apple, github 등)", content = @Content(schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "40001",
                                    "message": "지원하지 않는 소셜 로그인 제공자입니다."
                                }
                            }
                            """))),
                    @ApiResponse(responseCode = "401", description = "OIDC ID 토큰 서명 검증 실패 (토큰 위조 의심)", content = @Content(schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "40103",
                                    "message": "유효하지 않은 토큰입니다."
                                }
                            }
                            """))),
                    @ApiResponse(responseCode = "500", description = "구글/카카오/네이버 서버 다운 또는 응답 지연", content = @Content(schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "50001",
                                    "message": "소셜 로그인 서버와 통신 중 오류가 발생했습니다."
                                }
                            }
                            """))),
                    @ApiResponse(responseCode = "500", description = "우리 서버의 DB, Redis 연결 실패 등 내부 인프라 장애", content = @Content(schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "50000",
                                    "message": "서버 내부 오류입니다."
                                }
                            }
                            """)))
            }
    )
    @PostMapping("/login/{provider}")
    public CommonResponse<LoginResponse> login(
            @Parameter(description = "소셜 제공자 (kakao, google)", required = true) @PathVariable @NotBlank String provider,
            @Parameter(description = "소셜 서버로부터 받은 인가 코드", required = true) @RequestParam @NotBlank String code,
            @Parameter(description = "CSRF 방어용 state 값 (선택)") @RequestParam(required = false) String state) {

        LoginResponse response = authService.login(provider, code, state);
        return CommonResponse.ok(response);
    }

    @Operation(
            summary = "로그아웃",
            description = "사용자 로그아웃 (Redis에서 사용자의 리프레시 토큰 삭제. 프론트에서도 토큰 삭제 필수)",
            security = @SecurityRequirement(name = "BearerAuth"), // Access Token 필수
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": true,
                                "data": null,
                                "error": null
                            }
                            """))),
                    @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Access Token", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "40103",
                                    "message": "유효하지 않은 토큰입니다."
                                }
                            }
                            """))),
                    @ApiResponse(responseCode = "500", description = "Redis 서버 통신 장애로 인한 로그아웃 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "50000",
                                    "message": "서버 내부 오류입니다"
                                }
                            }
                            """)))
            }
    )
    @PostMapping("/logout")
    public CommonResponse<Void> logout(
            @Parameter(hidden = true) Authentication authentication) {
        // 필터를 통과하며 만들어진 인증 객체에서 유저 PK(Subject)를 꺼냅니다.
        String userId = authentication.getName();

        authService.logout(userId);

        // 데이터가 필요 없는 성공 응답
        return CommonResponse.ok(null);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "사용자의 계정 정보와 모든 연동된 보안 토큰을 영구 삭제합니다.",
            security = @SecurityRequirement(name = "BearerAuth"), // Access Token 필수
            responses = {
                    @ApiResponse(responseCode = "200", description = "탈퇴 성공", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": true,
                                "data": null,
                                "error": null
                            }
                            """))),
                    @ApiResponse(responseCode = "404", description = "DB에 존재하지 않는 사용자", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "40401",
                                    "message": "존재하지 않는 사용자입니다."
                                }
                            }
                            """))),
                    @ApiResponse(responseCode = "500", description = "DB 또는 Redis 통신 장애", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "INTERNAL_SERVER_ERROR",
                                    "message": "서버 내부 오류입니다."
                                }
                            }
                            """)))
            }
    )
    @DeleteMapping("/withdraw")
    public CommonResponse<Void> withdraw(
            @Parameter(hidden = true) Authentication authentication) {
        String userId = authentication.getName();

        authService.withdraw(userId);

        return CommonResponse.ok(null);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token으로 새로운 토큰 발급 (Access Token 만료시)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "재발급 성공", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": true,
                                "data": {
                                    "accessToken": "새로운_액세스_토큰...",
                                    "refreshToken": "기존_또는_새로운_리프레시_토큰...",
                                    "nickname": "한지수",
                                    "email": "jisuhan00@kakao.com"
                                },
                                "error": null
                            }
                            """))),
                    @ApiResponse(responseCode = "401", description = "리프레시 토큰 자체의 유효기간 만료 (재로그인 필요)", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "EXPIRED_TOKEN",
                                    "message": "JWT 토큰이 만료되었습니다"
                                }
                            }
                            """))),
                    @ApiResponse(responseCode = "401", description = "레디스 장부에 토큰이 없음 (로그아웃되었거나 TTL 만료됨. 재로그인 필요)", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "REVOKED_REFRESH_TOKEN",
                                    "message": "폐기되었거나 만료된 리프레시 토큰입니다."
                                }
                            }
                            """))),
                    @ApiResponse(responseCode = "401", description = "토큰 서명이 다르거나 레디스의 값과 불일치 (보안 경고)", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "INVALID_TOKEN",
                                    "message": "유효하지 않은 토큰입니다."
                                }
                            }
                            """))),
                    @ApiResponse(responseCode = "404", description = "토큰은 유효하지만 DB에서 해당 유저를 찾을 수 없음 (탈퇴한 유저 등)", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "NOT_FOUND_USER",
                                    "message": "존재하지 않는 사용자입니다."
                                }
                            }
                            """))),
                    @ApiResponse(responseCode = "500", description = "Redis 조회 실패 등 서버 내부 장애", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                            {
                                "success": false,
                                "data": null,
                                "error": {
                                    "code": "50000",
                                    "message": "서버 내부 오류입니다."
                                }
                            }
                            """)))
            }
    )
    @PostMapping("/reissue")
    public CommonResponse<LoginResponse> reissue(
            @Parameter(description = "Refresh Token (헤더 전달)", in = ParameterIn.HEADER, required = true)
            @RequestHeader("Refresh-Token") @NotBlank String refreshToken) {

        // 방금 전 우리가 완벽하게 예외 처리를 분리해둔 그 서비스 로직 호출!
        LoginResponse response = authService.reissue(refreshToken);
        return CommonResponse.ok(response);
    }
}
