package io.github.seoleeder.owls_pick.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI 번역 서버 대량 응답 DTO")
public record LocalizationBulkResponse(

        @Schema(description = "요청 성공 여부", example = "true")
        boolean success,

        @Schema(description = "한글화된 게임 결과 목록")
        List<ResultItem> results
) {
    @Schema(description = "한글화된 게임 데이터")
    public record ResultItem(

            @Schema(description = "게임 ID", example = "190264")
            @JsonProperty("game_id")
            Long gameId,

            @Schema(description = "한글화된 설명", example = "레드 데드 리뎀션 2는 근대화의 물결이...")
            @JsonProperty("description_ko")
            String descriptionKo,

            @Schema(description = "한글화된 스토리라인", example = "1899년 미국, 법 집행관들이...")
            @JsonProperty("storyline_ko")
            String storylineKo
    ) {}
}
