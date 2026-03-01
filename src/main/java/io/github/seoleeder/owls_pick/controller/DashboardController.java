package io.github.seoleeder.owls_pick.controller;

import io.github.seoleeder.owls_pick.dto.DashboardResponse;
import io.github.seoleeder.owls_pick.entity.game.Dashboard.CurationType;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "메인 대시보드 및 각종 차트 데이터 조회 API")
class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 특정 큐레이션 타입의 대시보드 차트 데이터 조회
     * @param type  조회할 큐레이션 타입 (WEEKLY_TOP_SELLER, MOST_PLAYED 등)
     * @param date  과거 데이터 조회를 위한 수집 기준 시각
     * @param limit 반환할 게임 리스트의 개수
     */
    @Operation(
            summary = "대시보드 차트 조회",
            description = "큐레이션 타입별 대시보드 조회. date 유뮤에 따라 최신 대시보드 데이터나 특정 수집 기준 시각 데이터 반환",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = DashboardResponse.class))),
                    @ApiResponse(responseCode = "500", description = "내부 인프라 장애", content = @Content(schema = @Schema(example = """
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
    @GetMapping("/{type}")
    public CommonResponse<DashboardResponse> getDashboard(
            @Parameter(description = "큐레이션 타입", example = "WEEKLY_TOP_SELLER")
            @PathVariable CurationType type,

            @Parameter(description = "수집 기준 시각 (과거 데이터 조회 시 필수)", example = "2026-02-10T09:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,

            @Parameter(description = "최대 조회 개수")
            @RequestParam(defaultValue = "10") int limit
    ) {
        // 특정 큐레이션 타입, 특정 수집 시각에 대한 대시보드 데이터 반환
        DashboardResponse response = dashboardService.getDashboard(type, date, limit);
        return CommonResponse.ok(response);
    }
}
