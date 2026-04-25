package io.github.seoleeder.owls_pick.dto.response.chat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Owls 챗봇 응답 반환용 DTO")
public record RagGenerationResponse(
        @Schema(description = "Vertex AI가 생성한 최종 텍스트 응답", example = "Outer Wilds를 강력히 추천합니다. 이 게임은...")
        String reply
) {}
