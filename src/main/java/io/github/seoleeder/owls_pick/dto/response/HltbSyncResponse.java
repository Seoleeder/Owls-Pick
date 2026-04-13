package io.github.seoleeder.owls_pick.dto.response;

import io.github.seoleeder.owls_pick.entity.game.enums.SyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "HLTB 플레이타임 데이터 동기화 응답 객체")
public record HltbSyncResponse(

        @Schema(description = "데이터 동기화 상태", example = "SUCCESS")
        @NotNull(message = "Scraping status is required")
        SyncStatus status,

        @Schema(description = "메인 스토리 (시간 단위)", example = "51.5")
        Double mainStory,

        @Schema(description = "메인 + 엑스트라 (시간 단위)", example = "103.0")
        Double mainExtra,

        @Schema(description = "올 클리어 (시간 단위)", example = "173.0")
        Double completionist
) {
    /**
     * FastAPI에서 넘어온 Double 형식의 시간을 Integer(분 단위)로 변환
     * 예: 51.5시간 -> 3090분
     */
    public Integer getMainStoryAsMinutes() {
        return mainStory != null ? (int) Math.round(mainStory * 60) : null;
    }

    public Integer getMainExtraAsMinutes() {
        return mainExtra != null ? (int) Math.round(mainExtra * 60) : null;
    }

    public Integer getCompletionistAsMinutes() {
        return completionist != null ? (int) Math.round(completionist * 60) : null;
    }
}
