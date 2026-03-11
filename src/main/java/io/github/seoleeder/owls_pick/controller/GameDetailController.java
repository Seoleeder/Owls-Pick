package io.github.seoleeder.owls_pick.controller;

import io.github.seoleeder.owls_pick.dto.response.GameDetailResponse;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.global.security.CustomUserDetails;
import io.github.seoleeder.owls_pick.service.GameDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/games") // 프로젝트 API 규격에 맞춰 수정해주세요
@RequiredArgsConstructor
@Tag(name = "Game Detail", description = "게임 상세 정보 조회 API")
public class GameDetailController {
    private final GameDetailService gameDetailService;

    @GetMapping("/{gameId}")
    @Operation(summary = "게임 상세 정보 조회", description = "특정 게임의 기본 정보, 스토어 상세 정보, 리뷰 통계, 언어 지원 등을 모두 포함한 상세 데이터 반환")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "상세 정보 조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "gameId": 190264,
                                        "title": "Red Dead Redemption 2",
                                        "titleLocalization": "레드 데드 리뎀션 2",
                                        "description": "레드 데드 리뎀션 2는 도망자 신세가 된 무법자 아서 모건과 반 더 린드 갱단의 이야기를 다루고 있습니다.",
                                        "storyline": "몰락해가는 서부 시대의 끝자락에서, 아서 모건이 추격과 내부 분열에 맞서 생존과 신념 사이의 위태로운 선택을 내리는 여정입니다.",
                                        "firstRelease": "2019-12-06",
                                        "coverId": "co1q1f",
                                        "ratingKr": "19 +",
                                        "ratingEsrb": "M",
                                        "isAdult": true,
                                        "mode": [
                                          "싱글",
                                          "멀티"
                                        ],
                                        "perspective": [
                                          "1인칭",
                                          "3인칭"
                                        ],
                                        "reviewSummary": "압도적인 디테일과 살아있는 오픈월드의 정점",
                                        "hypes": 257,
                                        "tags": {
                                          "genres": ["어드벤처", "RPG"],
                                          "themes": ["액션", "드라마", "오픈월드"],
                                          "keywords": ["승마", "낚시", "피"]
                                        },
                                        "playtime": {
                                          "mainStory": 58,
                                          "mainExtras": 102,
                                          "completionist": 135
                                        },
                                        "reviewStats": {
                                          "reviewScore": 9,
                                          "reviewScoreDesc": "압도적으로 긍정적",
                                          "totalPositive": 45000,
                                          "totalNegative": 3200,
                                          "totalReview": 48200,
                                          "reviewSummary": "압도적인 볼륨과 훌륭한 레벨 디자인을 갖춘 명작..."
                                        },
                                        "wishlist": {
                                          "isWished": true,
                                          "totalWishCount": 12500
                                        },
                                        "stores": [
                                          {
                                            "name": "Steam",
                                            "url": "https://store.steampowered.com/app/190264",
                                            "originalPrice": 64800,
                                            "discountPrice": 45360,
                                            "discountRate": 30,
                                            "expiryDate": "2026-04-01T23:59:59"
                                          }
                                        ],
                                        "languages": [
                                          {
                                            "language": "Korean",
                                            "voiceSupport": false,
                                            "subtitle": true,
                                            "interfaceSupport": true
                                          }
                                        ],
                                        "companies": [
                                          {
                                            "name": "Rockstar Games",
                                            "logo": "logo_rockstar",
                                            "isDeveloper": true,
                                            "isPublisher": true
                                          }
                                        ],
                                        "screenshots": [
                                          {
                                            "imageId": "scm8ru",
                                            "width": 1920,
                                            "height": 1080
                                          }
                                        ]
                                      },
                                      "error": null
                                    }
                                    """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 게임 (NOT_FOUND_GAME)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40402,
                                        "message": "존재하지 않는 게임입니다"
                                      }
                                    }
                                    """))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 50000,
                                        "message": "서버 내부 오류입니다."
                                      }
                                    }
                                    """))
            )
    })
    public CommonResponse<GameDetailResponse> getGameDetail(
            @Parameter(description = "게임 고유 ID", example = "190264", required = true)
            @PathVariable Long gameId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[GameDetailController] Request: Get details for Game ID = {}", gameId);

        Long userId = (userDetails != null) ? userDetails.getId() : null;

        // 서비스 로직 호출 및 CommonResponse 래핑 반환
        GameDetailResponse response = gameDetailService.getGameDetail(gameId, userId);

        log.info("[GameDetailController] Response: Successfully returned details for Game ID = {}", gameId);

        return CommonResponse.ok(response);
    }
}
