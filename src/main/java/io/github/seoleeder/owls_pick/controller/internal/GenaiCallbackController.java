package io.github.seoleeder.owls_pick.controller.internal;

import io.github.seoleeder.owls_pick.dto.response.EmbeddingBatchResponse;
import io.github.seoleeder.owls_pick.dto.response.KeywordLocalizationBulkResponse;
import io.github.seoleeder.owls_pick.dto.response.LocalizationBulkResponse;
import io.github.seoleeder.owls_pick.dto.response.ReviewSummaryResponse;
import io.github.seoleeder.owls_pick.global.response.CommonResponse;
import io.github.seoleeder.owls_pick.service.genai.EmbeddingService;
import io.github.seoleeder.owls_pick.service.genai.ReviewSummaryService;
import io.github.seoleeder.owls_pick.service.genai.localization.KeywordLocalizationService;
import io.github.seoleeder.owls_pick.service.genai.localization.LocalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "[INTERNAL] GenAI Callback", description = "GenAI 비동기 파이프라인 완료 콜백 수신 API")
@RestController
@RequestMapping("/api/internal/callback/genai")
@RequiredArgsConstructor
@Slf4j
public class GenaiCallbackController {

    private final LocalizationService localizationService;
    private final KeywordLocalizationService keywordLocalizationService;
    private final ReviewSummaryService reviewSummaryService;
    private final EmbeddingService embeddingService;

    @Operation(
            summary = "한글화 파이프라인 콜백 수신",
            description = "FastAPI에서 처리 완료된 한글화 데이터를 수신하여 대기 중인 파이프라인 스레드로 전달"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "콜백 수신 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": null,
                                      "error": null
                                    }
                                    """)
                    )),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 (필드 누락 또는 형식 불일치)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40000,
                                        "message": "잘못된 요청입니다."
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
    @PostMapping("/localization")
    public CommonResponse<Void> handleLocalizationCallback(
            @Valid @RequestBody LocalizationBulkResponse response) {

        log.info("[Callback] Received localized data callback for Task ID: {}", response.requestId());

        // 대기 중인 스레드에 콜백 결과 전달 및 트랜잭션 재개
        localizationService.completePendingTask(response);
        return CommonResponse.ok(null);
    }

    @Operation(
            summary = "키워드 한글화 완료 콜백 수신",
            description = "FastAPI에서 처리 완료된 키워드 데이터를 수신하여 대기 중인 파이프라인 스레드로 전달"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "콜백 수신 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": null,
                                      "error": null
                                    }
                                    """)
                    )),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 (필드 누락 또는 형식 불일치)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40000,
                                        "message": "잘못된 요청입니다."
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
    @PostMapping("/keywords")
    public CommonResponse<Void> handleKeywordLocalizationCallback(
            @Valid @RequestBody KeywordLocalizationBulkResponse response) {

        log.info("[Callback] Received keyword localization data callback for Request ID: {}", response.requestId());
        keywordLocalizationService.completePendingTask(response);
        return CommonResponse.ok(null);
    }

    @Operation(
            summary = "리뷰 요약 완료 콜백 수신",
            description = "FastAPI에서 처리 완료된 리뷰 요약 데이터를 수신하여 대기 중인 파이프라인 스레드로 전달"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "콜백 수신 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": null,
                                      "error": null
                                    }
                                    """)
                    )),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 (필드 누락 또는 형식 불일치)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40000,
                                        "message": "잘못된 요청입니다."
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
    @PostMapping("/reviews")
    public CommonResponse<Void> handleReviewSummaryCallback(
            @Valid @RequestBody ReviewSummaryResponse response) {

        log.info("[Callback] Received review summary data callback for Request ID: {}", response.requestId());
        reviewSummaryService.completePendingTask(response);
        return CommonResponse.ok(null);
    }

    @Operation(
            summary = "벡터 임베딩 완료 콜백 수신",
            description = "FastAPI에서 처리 완료된 벡터 임베딩 데이터를 수신하여 대기 중인 파이프라인 스레드로 전달"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "콜백 수신 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": null,
                                      "error": null
                                    }
                                    """)
                    )),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 (필드 누락 또는 형식 불일치)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": 40000,
                                        "message": "잘못된 요청입니다."
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
    @PostMapping("/embeddings")
    public CommonResponse<Void> handleEmbeddingCallback(
            @Valid @RequestBody EmbeddingBatchResponse response) {

        log.info("[Callback] Received embedding data callback for Request ID: {}", response.requestId());
        embeddingService.completePendingTask(response);
        return CommonResponse.ok(null);
    }
}