package io.github.seoleeder.owls_pick.controller.admin;

import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.service.genai.EmbeddingService;
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

@Tag(name = "[ADMIN] 벡터 임베딩 엔진 제어", description = "AI 벡터 임베딩 파이프라인 수동 제어 API (Required Header 'X-ADMIN-KEY')")
@RestController
@RequestMapping("/admin/embeddings")
@RequiredArgsConstructor
@Slf4j
public class EmbeddingController {
    private final EmbeddingService embeddingService;

    @Operation(
            summary = "벡터 임베딩 파이프라인 단일 청크 트리거",
            description = "아직 임베딩되지 않은(UNEMBEDDED) 게임 데이터를 조회 단위(dbFetchSize)만큼 가져와 임베딩 파이프라인 1회 가동",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "단일 청크 임베딩 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "processed_count": 1000
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
                                        "code": 40101,
                                        "message": "관리자 인증이 유효하지 않습니다."
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
                                        "code": 50002,
                                        "message": "FastAPI와의 통신에 실패했습니다."
                                      }
                                    }
                                    """))
            )
    })
    @PostMapping("/chunk-run")
    public CommonResponse<EmbeddingChunkResultDto> runChunkEmbedding(
            @RequestParam(defaultValue = "1000") int dbFetchSize
    ) {
        log.info("[Admin] Manual trigger requested for single chunk vector embedding. Target fetch size: {}", dbFetchSize);
        int processedCount = embeddingService.processDataChunk(dbFetchSize);
        return CommonResponse.ok(new EmbeddingChunkResultDto(processedCount));
    }

    @Operation(
            summary = "전체 벡터 임베딩 파이프라인 백그라운드 트리거 (Env Default)",
            description = "처리 대상 게임이 없을 때까지 YAML에 설정된 기본값으로 임베딩 파이프라인 무한 루프 가동",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "백그라운드 임베딩 파이프라인 가동 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": "Background Vector Embedding Pipeline Started",
                              "error": null
                            }
                            """)
                    )),
            @ApiResponse(responseCode = "401",
                    description = "관리자 인증 실패",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40101,
                                        "message": "관리자 인증이 유효하지 않습니다."
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
                                        "code": 50002,
                                        "message": "FastAPI와의 통신에 실패했습니다."
                                      }
                                    }
                                    """))
            )
    })
    @PostMapping("/run-all")
    public CommonResponse<String> runAllEmbedding() {
        log.info("[Admin] Manual trigger requested for ALL game vector embeddings (Default Config).");
        CompletableFuture.runAsync(() -> {
            try {
                embeddingService.runPipeline();
            } catch (Exception e) {
                log.error("[Admin] Default Vector Embedding Pipeline Failed: {}", e.getMessage());
            }
        });
        return CommonResponse.ok("Background Vector Embedding Pipeline Started");
    }

    @Operation(
            summary = "전체 벡터 임베딩 백그라운드 트리거 (Custom Fetch Size)",
            description = "DB 조회 사이즈를 직접 지정하여, 대상 게임이 없을 때까지 무한 루프 파이프라인 가동",
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
                              "data": "Custom Background Vector Embedding Pipeline Started",
                              "error": null
                            }
                            """)
                    )),
            @ApiResponse(responseCode = "401",
                    description = "관리자 인증 실패",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40101,
                                        "message": "관리자 인증이 유효하지 않습니다."
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
                                        "code": 50002,
                                        "message": "FastAPI와의 통신에 실패했습니다."
                                      }
                                    }
                                    """))
            )
    })
    @PostMapping("/run-all/custom")
    public CommonResponse<String> runAllEmbeddingWithCustomFetch(
            @RequestParam int dbFetchSize
    ) {
        log.info("[Admin] Manual trigger requested for ALL game vector embeddings with custom fetch size: {}", dbFetchSize);

        CompletableFuture.runAsync(() -> {
            try {
                embeddingService.runPipeline(dbFetchSize);
            } catch (Exception e) {
                log.error("[Admin] Custom Vector Embedding Pipeline Failed: {}", e.getMessage());
            }
        });

        return CommonResponse.ok("Custom Background Vector Embedding Pipeline Started");
    }

    /**
     * API 응답용 단일 청크 처리 결과 DTO
     */
    public record EmbeddingChunkResultDto(
            @Schema(description = "이번 청크에서 처리 대상이 된 게임 수", example = "1000")
            int processedCount
    ) {}
}
