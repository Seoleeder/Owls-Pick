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

            @NotEmpty(message = "Vector array must not be empty")
            @Schema(description = "Vertex AI에서 변환된 768차원 임베딩 벡터 배열")
            float[] vector,

            @NotBlank(message = "Source text must not be blank")
            @Schema(description = "FastAPI에서 동적으로 생성된 RAG용 프롬프트 원문")
            String sourceText,

            @NotBlank(message = "Embedding Status must not be blank")
            @Schema(description = "임베딩 처리 상태", example = "SUCCESS")
            EmbeddingStatus status
    ) {}
}
