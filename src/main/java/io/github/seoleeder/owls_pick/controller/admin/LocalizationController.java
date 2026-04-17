package io.github.seoleeder.owls_pick.controller.admin;

import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.service.genai.localization.KeywordLocalizationService;
import io.github.seoleeder.owls_pick.service.genai.localization.LocalizationService;
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

@Tag(name = "[ADMIN] 한글화 엔진 제어", description = "한글화 파이프라인 수동 제어 API (Required Header 'X-ADMIN-KEY')")
@RestController
@RequestMapping("/admin/localization")
@RequiredArgsConstructor
@Slf4j
public class LocalizationController {

    private final LocalizationService localizationService;
    private final KeywordLocalizationService keywordLocalizationService;

    @Operation(
            summary = "게임 데이터 대량 한글화 수동 트리거",
            description = "한글화되지 않은 게임 데이터(설명, 스토리라인)들을 지정된 단위(Chunk)만큼 가져와 한글화 로직 가동",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "한글화 요청 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "processed_count": 50
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
    public CommonResponse<LocalizationResultDto> runBulkLocalization(
            @RequestParam(defaultValue = "50") int chunkSize
    ) {
        log.info("[Admin] Manual trigger requested for bulk localization. Target chunk size: {}", chunkSize);
        int processedCount = localizationService.processLocalizationChunk(chunkSize);
        return CommonResponse.ok(new LocalizationResultDto(processedCount));
    }

    @Operation(
            summary = "게임 데이터 한글화 백그라운드 트리거 (Env Default)",
            description = "한글화되지 않은 게임이 하나도 없을 때까지 한글화 로직 가동 (청크 크기 고정)",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "백그라운드 한글화 가동 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": "Background Game Localization Pipeline Started",
                                  "error": null
                                }
                                """)
                    )),
            @ApiResponse(
                    responseCode = "401",
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
    public CommonResponse<String> runAllLocalization() {
        log.info("[Admin] Manual trigger requested for ALL game localization.");
        CompletableFuture.runAsync(() -> {
            try {
                localizationService.runPipeline();
            } catch (Exception e) {
                log.error("[Admin] Manual Game Localization Pipeline Failed: {}", e.getMessage());
            }
        });
        return CommonResponse.ok("Background Game Localization Pipeline Started");
    }

    @Operation(
            summary = "게임 키워드 한글화 수동 트리거",
            description = "한글화되지 않은 영문 키워드를 지정된 단위(Chunk)만큼 가져와 1회성 전체 한글화 파이프라인 실행",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "커스텀 키워드 한글화 가동 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "processedCount": 200
                                      },
                                      "error": null
                                    }
                                    """)
                    )),
            @ApiResponse(
                    responseCode = "401",
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
    @PostMapping("/bulk-run-keywords")
    public CommonResponse<LocalizationResultDto> runCustomKeywordLocalization(
            @RequestParam(defaultValue = "200") int chunkSize
    ) {
        log.info("[Admin] Manual trigger requested for Custom Keyword Localization with chunk size: {}", chunkSize);

        int processedCount = keywordLocalizationService.runPipeline(chunkSize, true);

        return CommonResponse.ok(new LocalizationResultDto(processedCount));
    }

    @Operation(
            summary = "게임 키워드 전체 한글화 백그라운드 트리거 (Env Default)",
            description = "한글화되지 않은 영문 키워드가 없을 때까지 전체 한글화 파이프라인 가동 (청크 사이즈 고정)",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "백그라운드 키워드 한글화 가동 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": "Background Keyword Localization Pipeline Started",
                                      "error": null
                                    }
                                    """)
                    )),
            @ApiResponse(
                    responseCode = "401",
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
    @PostMapping("/run-keywords")
    public CommonResponse<String> runKeywordLocalization() {
        log.info("[Admin] Manual trigger requested for Default Keyword Localization.");

        CompletableFuture.runAsync(() -> {
            try {
                int totalProcessed = keywordLocalizationService.runPipeline();
                log.info("[Admin] Background Keyword Localization Finished. Grand Total Processed: {}", totalProcessed);
            } catch (Exception e) {
                log.error("[Admin] Manual Default Keyword Localization Pipeline Failed: {}", e.getMessage());
            }
        });

        return CommonResponse.ok("Background Keyword Localization Pipeline Started");
    }

    // 응답용 내부 record DTO
    public record LocalizationResultDto(
            @Schema(description = "처리된 게임 수", example = "50")
            int processedCount
    ) {}
}