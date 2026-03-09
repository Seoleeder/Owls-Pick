package io.github.seoleeder.owls_pick.controller;

import io.github.seoleeder.owls_pick.dto.response.GameResponse;
import io.github.seoleeder.owls_pick.dto.response.TagResponse;
import io.github.seoleeder.owls_pick.entity.game.enums.GameSortType;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.service.ExploreTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/explore")
@RequiredArgsConstructor
@Tag(name = "태그 탐색 API", description = "장르 및 테마 기반 게임 큐레이션 탐색 API")
public class ExploreTagController {

    private final ExploreTagService exploreService;

    // 태그 (장르/테마) 목록 API
    @Operation(summary = "인기 장르 태그 조회 (top 5)", description = "메인 화면 노출용 상위 인기 장르 태그 목록 반환")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "code": "INDIE",
                                          "koreanName": "인디",
                                          "englishName": "Indie"
                                        },
                                        {
                                          "code": "ADVENTURE",
                                          "koreanName": "어드벤처",
                                          "englishName": "Adventure"
                                        }
                                      ],
                                      "error": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 50000,
                                        "message": "서버 내부 오류입니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/genres/popular")
    public CommonResponse<List<TagResponse>> getPopularGenres() {
        return CommonResponse.ok(exploreService.getPopularGenres());
    }

    @Operation(summary = "전체 장르 목록 조회", description = "탐색 페이지 '더보기'용 전체 장르 태그 목록 반환")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "code": "RPG",
                                          "koreanName": "롤플레잉",
                                          "englishName": "Role-playing (RPG)"
                                        }
                                      ],
                                      "error": null
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/genres")
    public CommonResponse<List<TagResponse>> getAllGenres() {
        return CommonResponse.ok(exploreService.getAllGenres());
    }

    @Operation(summary = "인기 테마 태그 조회 (top 5)", description = "메인 화면 노출용 상위 인기 테마 태그 목록 반환")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "code": "FANTASY",
                                          "koreanName": "판타지",
                                          "englishName": "Fantasy"
                                        }
                                      ],
                                      "error": null
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/themes/popular")
    public CommonResponse<List<TagResponse>> getPopularThemes() {
        return CommonResponse.ok(exploreService.getPopularThemes());
    }


    @Operation(summary = "더보기용 전체 테마 목록 조회", description = "탐색 페이지 '더보기'용 전체 장르 태그 목록 반환")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "code": "SCI_FI",
                                          "koreanName": "SF",
                                          "englishName": "Sci-Fi"
                                        }
                                      ],
                                      "error": null
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/themes")
    public CommonResponse<List<TagResponse>> getAllThemes() {
        return CommonResponse.ok(exploreService.getAllThemes());
    }

    // 특정 태그 탐색 페이지 게임 목록 조회 API
    @Operation(
            summary = "특정 장르의 게임 목록 조회 (페이지네이션)",
            description = "메인 페이지에서 선택한 장르의 게임 목록 조회. sort 파라미터로 정렬 조건 변경 가능"
    )
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
                            "gameId": 101,
                            "title": "Stardew Valley",
                            "coverUrl": "https://images.igdb.com/...",
                            "firstRelease": "2016-02-26",
                            "totalReview": 150000,
                            "reviewScore": 98,
                            "originalPrice": 16000,
                            "discountPrice": 8000,
                            "discountRate": 50
                          }
                        ],
                        "totalElements": 450,
                        "totalPages": 23,
                        "size": 20,
                        "number": 0
                      },
                      "error": null
                    }
                """))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 파라미터 요청",
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
    @GetMapping("/genres/{genre}/games")
    public CommonResponse<Page<GameResponse>> getGamesByGenre(
            @Parameter(description = "장르 코드", example = "RPG") @PathVariable GenreType genre,
            @Parameter(description = "정렬 기준", example = "POPULAR") @RequestParam(defaultValue = "POPULAR") GameSortType sort,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size);
        return CommonResponse.ok(exploreService.getGamesByGenre(genre, sort, pageable));
    }


    @Operation(
            summary = "특정 테마의 게임 목록 조회 (페이지네이션)",
            description = "선택한 테마의 게임 목록을 조회합니다. sort 파라미터를 통해 정렬 기준을 변경할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "content": [
                                          {
                                            "gameId": 54321,
                                            "title": "Cyberpunk 2077",
                                            "coverUrl": "https://images.igdb.com/...",
                                            "firstRelease": "2020-12-10",
                                            "totalReview": 650000,
                                            "reviewScore": 80,
                                            "originalPrice": 66000,
                                            "discountPrice": 33000,
                                            "discountRate": 50
                                          }
                                        ],
                                        "totalElements": 200,
                                        "totalPages": 10,
                                        "size": 20,
                                        "number": 0
                                      },
                                      "error": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 파라미터 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": "40001",
                                        "message": "잘못된 요청 파라미터입니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 50000,
                                        "message": "서버 내부 오류입니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/themes/{theme}/games")
    public CommonResponse<Page<GameResponse>> getGamesByTheme(
            @Parameter(description = "테마 코드", example = "FANTASY") @PathVariable ThemeType theme,
            @Parameter(description = "정렬 기준", example = "NEWEST") @RequestParam(defaultValue = "POPULAR") GameSortType sort,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size);
        return CommonResponse.ok(exploreService.getGamesByTheme(theme, sort, pageable));
    }
}