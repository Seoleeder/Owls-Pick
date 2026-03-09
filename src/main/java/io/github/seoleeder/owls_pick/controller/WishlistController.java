package io.github.seoleeder.owls_pick.controller;

import io.github.seoleeder.owls_pick.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.github.seoleeder.owls_pick.dto.response.WishlistToggleResponse;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games/{gameId}/wishlists")
public class WishlistController {
    private final WishlistService wishlistService;

    @Operation(summary = "위시리스트 토글", description = "특정 게임을 위시리스트에 추가하거나 해제. 해당 게임의 위시리스트 여부와 총 횟수를 묶어서 반환")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "위시리스트 토글 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success" : true,
                                      "data": {
                                        "isWished": true,
                                        "totalWishCount": 1204
                                      },
                                      "error": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "대상을 찾을 수 없음 (유저 또는 게임)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40402,
                                        "message": "존재하지 않는 사용자입니다"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
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
                                    """)))
    })
    @PostMapping
    public CommonResponse<WishlistToggleResponse> toggleWishlist(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "게임 ID", example = "1") @PathVariable Long gameId
    ) {

        //user id 추출
        Long userId = userDetails.getId();

        return CommonResponse.ok(wishlistService.toggleWishlist(userId, gameId));
    }
}
