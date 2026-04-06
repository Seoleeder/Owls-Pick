package io.github.seoleeder.owls_pick.controller.admin;

import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.service.ReviewSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Tag(name = "[ADMIN] 리뷰 요약 엔진 제어", description = "AI 리뷰 요약 파이프라인 수동 제어 API (Required Header 'X-ADMIN-KEY')")
@RestController
@RequestMapping("/admin/review-summary")
@RequiredArgsConstructor
@Slf4j
public class ReviewSummaryController {

    private final ReviewSummaryService reviewSummaryService;

    @Operation(
            summary = "게임 리뷰 요약 파이프라인 수동 트리거",
            description = "아직 요약되지 않은 게임 리뷰 데이터들을 배치 크기만큼 가져와 요약 파이프라인 1회 가동",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "리뷰 요약 요청 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "processed_count": 5
                                      },
                                      "error" : null
                                    }
                                    """)
                    )),
            @ApiResponse(responseCode = "401",
                    description = "관리자 인증 실패 (X-ADMIN-KEY 누락 또는 불일치)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40100,
                                        "message": "권한이 없습니다."
                                      }
                                    }
                                    """)
                    )),
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
    @PostMapping("/bulk-run")
    public CommonResponse<ReviewSummaryResultDto> runBulkSummary(
            @RequestParam(defaultValue = "5") int batchSize
    ) {
        log.info("[Admin] Manual trigger requested for bulk review summary. Target batch size: {}", batchSize);
        int processedCount = reviewSummaryService.processSingleBatch(batchSize);
        return CommonResponse.ok(new ReviewSummaryResultDto(processedCount));
    }

    @Operation(
            summary = "게임 리뷰 요약 파이프라인 백그라운드 트리거 (Env Default)",
            description = "요약 대상 게임이 하나도 없을 때까지 AI 요약 로직 무한 루프 가동 (배치 크기 고정)",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "백그라운드 리뷰 요약 가동 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": "Background Review Summary Pipeline Started",
                              "error": null
                            }
                            """)
                    )),
            @ApiResponse(responseCode = "401",
                    description = "관리자 인증 실패 (X-ADMIN-KEY 누락 또는 불일치)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40100,
                                        "message": "권한이 없습니다."
                                      }
                                    }
                                    """)
                    )),
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
    @PostMapping("/run-all")
    public CommonResponse<String> runAllSummary() {
        log.info("[Admin] Manual trigger requested for ALL game review summary.");
        CompletableFuture.runAsync(() -> {
            try {
                reviewSummaryService.runPipeline();
            } catch (Exception e) {
                log.error("[Admin] Manual Review Summary Pipeline Failed: {}", e.getMessage());
            }
        });
        return CommonResponse.ok("Background Review Summary Pipeline Started");
    }

    @Operation(
            summary = "리뷰 전체 요약 백그라운드 트리거 (Custom Batch Size)",
            description = "배치 사이즈를 직접 지정하여, 요약 대상 게임이 없을 때까지 무한 루프 파이프라인 가동",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "커스텀 설정 백그라운드 파이프라인 가동 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": "Custom Background Review Summary Pipeline Started",
                              "error": null
                            }
                            """)
                    )),
            @ApiResponse(responseCode = "401",
                    description = "관리자 인증 실패 (X-ADMIN-KEY 누락 또는 불일치)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40100,
                                        "message": "권한이 없습니다."
                                      }
                                    }
                                    """)
                    )),
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
    @PostMapping("/run-all/custom")
    public CommonResponse<String> runAllSummaryWithCustomBatch(
            @RequestParam int batchSize
    ) {
        log.info("[Admin] Manual trigger requested for ALL game review summary with custom batch size: {}", batchSize);

        CompletableFuture.runAsync(() -> {
            try {
                reviewSummaryService.runPipeline(batchSize);
            } catch (Exception e) {
                log.error("[Admin] Custom Review Summary Pipeline Failed: {}", e.getMessage());
            }
        });

        return CommonResponse.ok("Custom Background Review Summary Pipeline Started");
    }

    // 응답용 내부 record DTO
    public record ReviewSummaryResultDto(
            @Schema(description = "처리된 게임 수", example = "5")
            int processedCount
    ) {}
}