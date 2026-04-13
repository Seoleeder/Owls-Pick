package io.github.seoleeder.owls_pick.controller.admin;


import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.service.client.hltb.HltbSyncService;
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

@Slf4j
@Tag(name = "[ADMIN] HLTB 수집 파이프라인", description = "HowLongToBeat 플레이타임 데이터 수집 및 제어")
@RestController
@RequestMapping("/admin/hltb")
@RequiredArgsConstructor
public class HltbController {

    private final HltbSyncService hltbSyncService;

    @Operation(summary = "HLTB 전체 동기화 백그라운드 트리거 (Env Default)",
            description = "대상 데이터가 소진될 때까지 무한 루프 파이프라인 가동 (환경변수 설정 청크 사이즈)",
            parameters = @Parameter(name = "X-ADMIN-KEY", in = ParameterIn.HEADER, required = true))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전체 동기화 시작 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                        {
                          "success": true,
                          "data": "Full Sync Started in Background",
                          "error": null
                        }
                        """))),
            @ApiResponse(responseCode = "401", description = "관리자 인증 실패",
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
                                    """))),
            @ApiResponse(responseCode = "500", description = "FastAPI 서버 오류",
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
                                    """)))
    })
    @PostMapping("/sync/all")
    public CommonResponse<String> syncAllDefault() {
        log.info("[Admin] HLTB Full Sync Started (Default Config)");

        // 전체 처리는 비동기로 던짐
        CompletableFuture.runAsync(hltbSyncService::runSyncPipeline);

        return CommonResponse.ok("Full Sync Started in Background");
    }

    @Operation(summary = "HLTB 전체 동기화 백그라운드 트리거 (Custom Chunk Size)",
            description = "배치 사이즈 지정 후 데이터 소진 시까지 무한 루프 파이프라인 가동",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "커스텀 백그라운드 동기화 시작 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": "Custom Background HLTB Sync Pipeline Started",
                                      "error": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "관리자 인증 실패",
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
                                    """))),
            @ApiResponse(responseCode = "500", description = "FastAPI 서버 오류",
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
                                    """)))
    })
    @PostMapping("/sync/all/{chunkSize}")
    public CommonResponse<String> syncAllCustom(@PathVariable int chunkSize) {
        log.info("[Admin] HLTB Full Sync Started (Chunk Size: {})", chunkSize);

        CompletableFuture.runAsync(() -> {
            try {
                hltbSyncService.runSyncPipeline(chunkSize);
            } catch (Exception e) {
                log.error("[Admin] Custom HLTB Pipeline Failed: {}", e.getMessage());
            }
        });

        return CommonResponse.ok("Custom Background HLTB Sync Pipeline Started");
    }

    @Operation(summary = "HLTB 단일 배치 동기화 수동 트리거",
            description = "미동기화 게임 데이터를 배치 크기만큼 조회하여 HLTB 데이터 동기화 1회 가동",
            parameters = {
                    @Parameter(name = "X-ADMIN-KEY", description = "관리자 키", required = true, in = ParameterIn.HEADER)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "단일 배치 동기화 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "processed_count": 10
                                      },
                                      "error": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "관리자 인증 실패",
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
                                    """))),
            @ApiResponse(responseCode = "500", description = "FastAPI 서버 오류",
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
                                    """)))
    })
    @PostMapping("/sync/once/{chunkSize}")
    public CommonResponse<HltbSyncResultDto> syncOnce(@PathVariable int chunkSize) {
        log.info("[Admin] HLTB Single Batch Execution Requested (Size: {})", chunkSize);

        int processedCount = hltbSyncService.runSingleBatchSync(chunkSize);
        return CommonResponse.ok(new HltbSyncResultDto(processedCount));
    }

    public record HltbSyncResultDto(
            @Schema(description = "처리된 게임 수", example = "10")
            int processedCount
    ) {}
}
