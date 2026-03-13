package io.github.seoleeder.owls_pick.controller;

import io.github.seoleeder.owls_pick.dto.response.section.PersonalizedSectionResponse;
import io.github.seoleeder.owls_pick.dto.response.section.UpcomingSectionResponse;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.global.security.CustomUserDetails;
import io.github.seoleeder.owls_pick.service.MainPickService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "사용자 맞춤형 섹션 API", description = "메인 페이지에 쓰일 사용자 맞춤형 게임 추천 섹션 API")
@RestController
@RequestMapping("/api/main-picks")
@RequiredArgsConstructor
public class MainPickController {

    private final MainPickService mainPickService;

    @Operation(summary = "출시 예정 기대작", description = "몇 개월 내 출시 예정이며, Hypes(글로벌 유저 기대도를 나타내는)가 높은 기대작 게임 리스트 반환 ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "titleKeyword": "출시 예정 기대작",
                        "games": {
                          "content": [
                            {
                              "gameId": 274687,
                              "title": "Crimson Desert",
                              "coverUrl": "https://images.igdb.com/igdb/image/upload/t_cover_big/co4321.jpg",
                              "firstRelease": "2025-03-19",
                              "hypes": 244,
                              "platforms": [Xbox Series X|S,PC (Microsoft Windows),PlayStation 5,Mac]
                            },
                            {
                              "gameId": 242639,
                              "title": "Forza Horizon 6",
                              "coverUrl": "https://images.igdb.com/igdb/image/upload/t_cover_big/co8765.jpg",
                              "firstRelease": "2026-05-19",
                              "hypes": 62,
                              "platforms": [Xbox Series X|S,PC (Microsoft Windows),PlayStation 5]
                            }
                          ],
                          "totalElements": 250,
                          "totalPages": 13,
                          "size": 20,
                          "number": 0
                        }
                      },
                      "error": null
                    }
                    """))),
            @ApiResponse(
                    responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": 50000,
                        "message": "서버 내부 오류입니다."
                      }
                    }
                """)))
    })
    @GetMapping("/upcoming")
    public CommonResponse<UpcomingSectionResponse> getUpcomingGames(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size);
        UpcomingSectionResponse result = mainPickService.getUpcomingGames(pageable);
        return CommonResponse.ok(result);
    }

    @Operation(summary = "[Section 1] 선호 태그 기반 맞춤 추천", description = "유저의 선호 태그를 가장 많이 포함하는 맞춤형 게임 리스트 반환")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": {
                                "titleKeyword": "맞춤 픽",
                                "games": {
                                  "content": [
                                    {
                                      "gameId": 1,
                                      "title": "Elden Ring",
                                      "coverUrl": "https://images.igdb.com/...",
                                      "firstRelease": "2022-02-25",
                                      "totalReview": 500000,
                                      "reviewScore": 95,
                                      "originalPrice": 64800,
                                      "discountPrice": 45000,
                                      "discountRate": 30
                                    }
                                  ],
                                  "totalElements": 50,
                                  "totalPages": 3,
                                  "size": 20,
                                  "number": 0
                                }
                              },
                              "error": null
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": "40401",
                                "message": "존재하지 않는 사용자입니다."
                              }
                            }
                            """))),
            @ApiResponse(
                    responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": 50000,
                        "message": "서버 내부 오류입니다."
                      }
                    }
                """)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/most-personalized")
    public CommonResponse<PersonalizedSectionResponse> getMostPersonalizedPicks(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        // 현재 로그인된 사용자 ID 추출
        Long userId = (userDetails != null) ? userDetails.getId() : null;

        PageRequest pageable = PageRequest.of(page, size);

        PersonalizedSectionResponse result = mainPickService.getMostPersonalizedPicks(userId, pageable);
        return CommonResponse.ok(result);
    }


    @Operation(summary = "[Section 2] 단일 장르 랜덤 탐색", description = "서버에서 무작위로 선택한 단일 장르의 인기 게임 리스트 반환")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                        {
                          "success": true,
                          "data": {
                            "titleKeyword": "어드벤쳐",
                            "games": {
                              "content": [
                                {
                                  "gameId": 2,
                                  "title": "Sekiro™: Shadows Die Twice - GOTY Edition",
                                  "coverUrl": "https://images.igdb.com/...",
                                  "firstRelease": "2019-03-22",
                                  "totalReview": 120000,
                                  "reviewScore": 8,
                                  "originalPrice": 75000,
                                  "discountPrice": 18750,
                                  "discountRate": 75
                                }
                              ],
                              "totalElements": 120,
                              "totalPages": 6,
                              "size": 20,
                              "number": 0
                            }
                          },
                          "error": null
                        }
                        """))),
        @ApiResponse(
                responseCode = "500", description = "서버 내부 오류",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": 50000,
                                "message": "서버 내부 오류입니다."
                              }
                            }
                        """)))
    })
    @GetMapping("/random-genre")
    public CommonResponse<PersonalizedSectionResponse> getRandomGenrePicks(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size);

        PersonalizedSectionResponse result = mainPickService.getRandomGenrePicks(pageable);
        return CommonResponse.ok(result);
    }


    @Operation(summary = "[Section 3] 단일 테마 랜덤 탐색", description = "서버에서 무작위로 선택한 단일 테마의 인기 게임 리스트를 반환합니다. (미성년자는 EROTIC 자동 배제)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": {
                                "titleKeyword": "오픈월드",
                                "games": {
                                  "content": [
                                    {
                                      "gameId": 3,
                                      "title": "Cyberpunk 2077",
                                      "coverUrl": "https://images.igdb.com/...",
                                      "firstRelease": "2020-12-10",
                                      "totalReview": 650000,
                                      "reviewScore": 8,
                                      "originalPrice": 66000,
                                      "discountPrice": 33000,
                                      "discountRate": 50
                                    }
                                  ],
                                  "totalElements": 200,
                                  "totalPages": 10,
                                  "size": 20,
                                  "number": 0
                                }
                              },
                              "error": null
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": "40401",
                                "message": "존재하지 않는 사용자입니다."
                              }
                            }
                            """))),
            @ApiResponse(
                    responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": 50000,
                        "message": "서버 내부 오류입니다."
                      }
                    }
                """)))
    })
    @GetMapping("/random-theme")
    public CommonResponse<PersonalizedSectionResponse> getRandomThemePicks(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        // 현재 로그인된 사용자 ID 추출
        Long userId = (userDetails != null) ? userDetails.getId() : null;

        PageRequest pageable = PageRequest.of(page, size);

        PersonalizedSectionResponse result = mainPickService.getRandomThemePicks(userId, pageable);
        return CommonResponse.ok(result);
    }


    @Operation(summary = "[Section 4] 유효 조합(장르 X 테마) 랜덤 탐색", description = "유효한 (데이터가 보장된) 장르와 테마 조합을 가진 게임 리스트 반환")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                        {
                          "success": true,
                          "data": {
                            "titleKeyword": "인디 오픈월드",
                            "games": {
                              "content": [
                                {
                                  "gameId": 4,
                                  "title": "Peak",
                                  "coverUrl": "https://images.igdb.com/...",
                                  "firstRelease": "2025-06-16",
                                  "totalReview": 8000,
                                  "reviewScore": 8,
                                  "originalPrice": 8400,
                                  "discountPrice": 8400,
                                  "discountRate": 0
                                }
                              ],
                              "totalElements": 85,
                              "totalPages": 5,
                              "size": 20,
                              "number": 0
                            }
                          },
                          "error": null
                        }
                        """))),
        @ApiResponse(
                responseCode = "500", description = "서버 내부 오류",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": 50000,
                                "message": "서버 내부 오류입니다."
                              }
                            }
                        """)))
    })
    @GetMapping("/random-intersection")
    public CommonResponse<PersonalizedSectionResponse> getIntersectionPicks(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size);

        PersonalizedSectionResponse result = mainPickService.getIntersectionPicks(pageable);
        return CommonResponse.ok(result);
    }


    @Operation(summary = "[Section 5] 숨겨진 명작", description = "스코어는 높지만 리뷰 수가 상대적으로 적은 숨겨진 명작 게임 리스트 반환")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                        {
                          "success": true,
                          "data": {
                            "titleKeyword": "숨겨진 명작",
                            "games": {
                              "content": [
                                {
                                  "gameId": 5,
                                  "title": "Desynced",
                                  "coverUrl": "https://images.igdb.com/...",
                                  "firstRelease": "2026-03-05",
                                  "totalReview": 1300,
                                  "reviewScore": 8,
                                  "originalPrice": 32000,
                                  "discountPrice": 19200,
                                  "discountRate": 40
                                }
                              ],
                              "totalElements": 30,
                              "totalPages": 2,
                              "size": 20,
                              "number": 0
                            }
                          },
                          "error": null
                        }
                        """))),
        @ApiResponse(
                responseCode = "500", description = "서버 내부 오류",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": 50000,
                                "message": "서버 내부 오류입니다."
                              }
                            }
                        """)))
    })
    @GetMapping("/hidden-masterpieces")
    public CommonResponse<PersonalizedSectionResponse> getHiddenMasterpieces(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size);
        return CommonResponse.ok(mainPickService.getHiddenMasterpieces(pageable));
    }


    @Operation(summary = "[Section 6] 트렌딩 픽", description = "유저의 선호 태그 중 하나를 선정하여, 최근 일주일간 리뷰가 급증한 게임 리스트 반환")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "titleKeyword": "생존",
                        "games": {
                          "content": [
                            {
                              "gameId": 6,
                              "title": "Palworld",
                              "coverUrl": "https://images.igdb.com/...",
                              "firstRelease": "2024-01-19",
                              "totalReview": 20329,
                              "reviewScore": 9,
                              "originalPrice": 32000,
                              "discountPrice": 32000,
                              "discountRate": 0
                            }
                          ],
                          "totalElements": 40,
                          "totalPages": 2,
                          "size": 20,
                          "number": 0
                        }
                      },
                      "error": null
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": "40401",
                                "message": "존재하지 않는 사용자입니다."
                              }
                            }
                            """))),
            @ApiResponse(
                    responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": 50000,
                        "message": "서버 내부 오류입니다."
                      }
                    }
                """)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/trending")
    public CommonResponse<PersonalizedSectionResponse> getTrendingPicks(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        Long userId = (userDetails != null) ? userDetails.getId() : null;
        PageRequest pageable = PageRequest.of(page, size);
        PersonalizedSectionResponse result = mainPickService.getTrendingPicks(userId, pageable);
        return CommonResponse.ok(result);
    }


    @Operation(summary = "[Section 7] 퀵 플레이 (짧은 플탐)", description = "유저의 선호 태그 중 하나를 선정하여, 플레이 타임이 짧고 강렬한 고평점 게임을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "titleKeyword": "인디",
                        "games": {
                          "content": [
                            {
                              "gameId": 7,
                              "title": "Vampire Survivors",
                              "coverUrl": "https://images.igdb.com/...",
                              "firstRelease": "2021-12-17",
                              "totalReview": 210000,
                              "reviewScore": 98,
                              "originalPrice": 5000,
                              "discountPrice": 4000,
                              "discountRate": 20
                            }
                          ],
                          "totalElements": 60,
                          "totalPages": 3,
                          "size": 20,
                          "number": 0
                        }
                      },
                      "error": null
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": "40401",
                                "message": "존재하지 않는 사용자입니다."
                              }
                            }
                            """))),
            @ApiResponse(
                    responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": 50000,
                        "message": "서버 내부 오류입니다."
                      }
                    }
                """)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/quick-plays")
    public CommonResponse<PersonalizedSectionResponse> getQuickPlays(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        Long userId = (userDetails != null) ? userDetails.getId() : null;
        PageRequest pageable = PageRequest.of(page, size);
        PersonalizedSectionResponse result = mainPickService.getQuickPlays(userId, pageable);
        return CommonResponse.ok(result);
    }
}
