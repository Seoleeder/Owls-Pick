package io.github.seoleeder.owls_pick.controller;

import io.github.seoleeder.owls_pick.dto.request.NotificationToggleRequest;
import io.github.seoleeder.owls_pick.dto.request.ProfileUpdateRequest;
import io.github.seoleeder.owls_pick.dto.response.MyPageResponse;
import io.github.seoleeder.owls_pick.dto.response.WishlistResponse;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.global.security.CustomUserDetails;
import io.github.seoleeder.owls_pick.service.UserProfileService;
import io.github.seoleeder.owls_pick.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이 페이지 API", description = "마이 페이지 통합 관리 API (프로필, 위시리스트)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

    private final UserProfileService userProfileService;
    private final WishlistService wishlistService;

    // ==========================================
    // 프로필 및 설정 영역
    // ==========================================

    @Operation(summary = "마이 페이지 프로필 조회", description = "닉네임, 이메일, 선호 태그 및 스토어, 할인 알림 동의 여부 조회")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "provider": "GOOGLE",
                                        "nickname": "도찌1031",
                                        "email": "user@gmail.com",
                                        "isDiscountNotificationEnabled": true,
                                        "preferredTags": ["RPG", "Action", "SF"],
                                        "preferredStores": ["STEAM", "EPIC_GAMES_STORE"]
                                      },
                                      "error": null
                                    }
                                    """))),
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
    @GetMapping("/profile")
    public CommonResponse<MyPageResponse> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return CommonResponse.ok(userProfileService.getMyPage(userDetails.getId()));
    }


    @Operation(summary = "마이 페이지 프로필 수정 (PATCH)", description = "변경을 원하는 필드만 선택적으로 전송하여 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": null,
                                      "error": null
                                    }
                                    """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 입력값 형식 (유효성 검사 실패)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": "40001",
                                        "message": "잘못된 요청 파라미터입니다."
                                      }
                                    }
                                    """))),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 사용 중인 닉네임",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": "40902",
                                        "message": "이미 사용 중인 닉네임입니다."
                                      }
                                    }
                                    """))),
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
    @PatchMapping("/profile")
    public CommonResponse<Void> updateMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        userProfileService.updateMyProfile(userDetails.getId(), request);
        return CommonResponse.ok(null);
    }

    @Operation(summary = "알림 설정 토글 (PATCH)", description = "할인 알림 수신 동의 여부를 변경합니다. 비동의 처리 시 서버에 저장된 사용자의 기기 토큰이 즉시 삭제됩니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": null,
                                      "error": null
                                    }
                                    """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 입력값 형식 (필수값 누락)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": "40001",
                                        "message": "Discount notification setting is required."
                                      }
                                    }
                                    """))),
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
    @PatchMapping("/notification")
    public CommonResponse<Void> toggleNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationToggleRequest request
    ) {
        userProfileService.toggleDiscountNotification(userDetails.getId(), request);
        return CommonResponse.ok(null);
    }

    // ==========================================
    // 위시리스트(찜 목록) 영역
    // ==========================================

    @Operation(summary = "내 찜 목록 조회", description = "마이 페이지 하단에 노출될 위시리스트를 페이징하여 조회")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "content": [
                                          {
                                            "wishedAt": "2026-03-13T15:00:00",
                                            "game": {
                                              "gameId": 105,
                                              "title": "Stardew Valley",
                                              "coverUrl": "https://images.igdb.com/igdb/image/upload/t_cover_big/co1x7d.jpg",
                                              "firstRelease": "2016-02-26",
                                              "totalReview": 150000,
                                              "reviewScore": 98,
                                              "originalPrice": 16000,
                                              "discountPrice": 8000,
                                              "discountRate": 50
                                            }
                                          }
                                        ],
                                        "pageable": "INSTANCE",
                                        "totalElements": 1,
                                        "totalPages": 1,
                                        "last": true,
                                        "size": 10,
                                        "number": 0,
                                        "numberOfElements": 1,
                                        "first": true,
                                        "empty": false
                                      },
                                      "error": null
                                    }
                                    """))),
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
    @GetMapping("/wishlists")
    public CommonResponse<Page<WishlistResponse>> getMyWishlists(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 데이터 개수", example = "10") @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return CommonResponse.ok(wishlistService.getMyWishlist(userDetails.getId(), pageable));
    }

    @Operation(summary = "찜 목록에서 개별 삭제", description = "위시리스트에서 특정 게임 삭제")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": null,
                                      "error": null
                                    }
                                    """))),
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
    @DeleteMapping("/wishlists/{gameId}")
    public CommonResponse<Void> removeWishlist(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long gameId
    ) {
        wishlistService.removeFromWishlist(userDetails.getId(), gameId);
        return CommonResponse.ok(null);
    }
}
