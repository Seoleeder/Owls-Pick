package io.github.seoleeder.owls_pick.controller;

import io.github.seoleeder.owls_pick.dto.request.GameSearchConditionRequest;
import io.github.seoleeder.owls_pick.dto.response.GameResponse;
import io.github.seoleeder.owls_pick.dto.response.SearchFilterMetadataResponse;
import io.github.seoleeder.owls_pick.service.GameSearchService;
import io.github.seoleeder.owls_pick.global.response.CommonResponse; // CommonResponse 경로
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Tag(name = "Game Search", description = "게임 통합 검색 및 필터링 API")
public class GameSearchController {

    private final GameSearchService gameSearchService;

    @GetMapping("/metadata")
    @Operation(summary = "검색 필터 메타데이터 조회", description = "프론트엔드 필터 UI 구성을 위한 장르, 테마 목록 및 가격/플레이타임 범위를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "메타데이터 조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isok": true,
                                      "code": "ok",
                                      "message": "성공",
                                      "data": {
                                        "genres": [
                                          { "code": "ACTION", "korName": "액션" }
                                        ],
                                        "themes": [
                                          { "code": "FANTASY", "korName": "판타지" }
                                        ],
                                        "priceRange": { "min": 0, "max": 105000 },
                                        "playtimeRange": { "min": 0, "max": 300 }
                                      }
                                    }
                                    """)))
    })
    public CommonResponse<SearchFilterMetadataResponse> getSearchMetadata() {
        return CommonResponse.ok(gameSearchService.getSearchMetadata());
    }

    @PostMapping
    @Operation(summary = "게임 통합 검색", description = "주어진 다중 조건(키워드, 장르, 테마, 가격 범위 등)을 조합하여 게임을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isok": true,
                                      "code": "ok",
                                      "message": "성공",
                                      "data": {
                                        "content": [
                                          {
                                            "gameId": 1,
                                            "title": "엘든 링",
                                            "coverUrl": "https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.jpg",
                                            "firstRelease": "2022-02-25",
                                            "totalReview": 50000,
                                            "reviewScore": 95,
                                            "originalPrice": 64800,
                                            "discountPrice": 45360,
                                            "discountRate": 30
                                          }
                                        ],
                                        "pageable": {
                                          "pageNumber": 0,
                                          "pageSize": 20
                                        },
                                        "totalElements": 1,
                                        "totalPages": 1,
                                        "last": true
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 검색 조건 (예: 최소 가격이 최대 가격보다 큼)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isok": false,
                                      "code": "INVALID_PRICE_RANGE",
                                      "message": "최소 가격은 최대 가격보다 클 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    public CommonResponse<Page<GameResponse>> searchGames(
            @Valid @RequestBody GameSearchConditionRequest condition,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        // 컨트롤러 내부에서 명시적으로 PageRequest를 생성하여 서비스로 전달
        Pageable pageable = PageRequest.of(page, size);

        return CommonResponse.ok(gameSearchService.searchGames(condition, pageable));
    }
}