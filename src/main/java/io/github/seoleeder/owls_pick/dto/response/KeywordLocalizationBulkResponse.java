package io.github.seoleeder.owls_pick.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * FastAPI로부터 반환받는 키워드 한글화 리스트 DTO
 */
@Schema(description = "GenAI 키워드 한글화 대량 응답 DTO")
public record KeywordLocalizationBulkResponse(
        @Schema(description = "키워드 한글화 결과 목록")
        List<KeywordLocalizationResponse> localizationResults
) {
    @Schema(description = "단일 키워드 한글화 결과")
    public record KeywordLocalizationResponse(
            @Schema(description = "원본 영문 키워드", example = "action")
            String engName,

            @Schema(description = "한글화된 키워드", example = "액션")
            String korName,

            @Schema(description = "한글화 실패 사유", example = "SAFETY_FILTER_REJECTED")
            String errorReason
    ) {}
}
