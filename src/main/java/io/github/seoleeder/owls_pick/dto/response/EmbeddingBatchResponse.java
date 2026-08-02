package io.github.seoleeder.owls_pick.dto.response;

import io.github.seoleeder.owls_pick.entity.game.enums.status.EmbeddingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "벡터 임베딩 배치 응답 DTO")
public record EmbeddingBatchResponse(
        @Schema(description = "비동기 콜백 매핑용 요청 식별자", example = "550e8400-e29b-41d4-a716-446655440000")
        String requestId,

        @Valid
        @NotEmpty(message = "Embedded results must not be empty")
        @Schema(description = "처리가 완료된 임베딩 게임 목록")
        List<EmbeddedGame> results
) {
    @Schema(description = "임베딩 완료된 게임 데이터")
    public record EmbeddedGame(
            @NotNull(message = "Game ID must not be null")
            @Schema(description = "게임 ID", example = "12345")
            Long gameId,

            @Schema(description = "임베딩 엔진에서 변환된 768차원 벡터 배열")
            float[] vector,

            @NotBlank(message = "Embedding Status must not be blank")
            @Schema(description = "임베딩 처리 상태", example = "SUCCESS")
            EmbeddingStatus status,

            @Schema(description = "임베딩 작업 실패 사유", example = "INSUFFICIENT_DATA")
            String errorReason
    ) {}
}
