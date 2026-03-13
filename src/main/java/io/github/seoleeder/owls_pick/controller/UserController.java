package io.github.seoleeder.owls_pick.controller;

import io.github.seoleeder.owls_pick.dto.request.OnboardingRequest;
import io.github.seoleeder.owls_pick.dto.response.UserStatusResponse;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.global.security.CustomUserDetails;
import io.github.seoleeder.owls_pick.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "유저 API", description = "사용자 프로필 및 온보딩 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @Operation(
            summary = "현재 사용자 상태 조회",
            description = "로그인한 유저의 온보딩 완료 여부 및 성인 인증 여부를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            value = """
            {
              "success": true,
              "data": {
                "userId": 1,
                "isOnboarded": true,
                "isAdult": true
              },
              "error": null
            }
            """
                    ))
            )
    })
    @ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(
                    value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": "50000",
                                "message": "서버 내부 오류입니다."
                              }
                            }
                            """
            ))
    )
    @GetMapping("/status")
    public CommonResponse<UserStatusResponse> getUserStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 1. Principal에서 ID 추출 후 서비스 호출
        UserStatusResponse response = userProfileService.getUserStatus(userDetails.getId());

        // 2. 표준 응답 객체에 담아 반환
        return CommonResponse.ok(response);
    }

    @Operation(
            summary = "사용자 온보딩 처리",
            description = "최초 로그인 유저의 생년월일, 선호 태그 및 스토어 데이터 등록"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "온보딩 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            value = """
                            {
                              "success": true,
                              "data": null,
                              "error": null
                            }
                            """
                    ))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "상태 충돌: 이미 온보딩을 완료한 사용자",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": "40901",
                                "message": "이미 온보딩이 완료되었습니다."
                              }
                            }
                            """
                    ))
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": "50000",
                                "message": "서버 내부 오류입니다."
                              }
                            }
                            """
                    ))
            )
    })
    @PatchMapping("/onboarding")
    public CommonResponse<Void> onboardUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingRequest request
    ) {

        Long userId = userDetails.getId();

        // 1. 서비스 로직 호출 (예외 발생 시 GlobalExceptionHandler가 처리)
        userProfileService.completeOnboarding(userId, request);

        // 2. ResponseEntity 없이 CommonResponse 객체만 깔끔하게 직접 반환
        return CommonResponse.ok(null);
    }

    @Operation(summary = "닉네임 중복 확인", description = "온보딩 및 프로필 수정 시 닉네임 중복 여부 확인")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (data: true면 사용 가능, false면 중복)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            value = """
                            {
                              "success": true,
                              "data": true,
                              "error": null
                            }
                            """
                    ))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": "50000",
                                "message": "서버 내부 오류입니다."
                              }
                            }
                            """
                    ))
            )
    })
    @GetMapping("/check-nickname")
    public CommonResponse<Boolean> checkNickname(
            @Parameter(description = "검사할 닉네임", example = "도찌1031", required = true)
            @RequestParam("nickname") String nickname
    ) {
        boolean isAvailable = userProfileService.isNicknameAvailable(nickname);
        return CommonResponse.ok(isAvailable);
    }
}
