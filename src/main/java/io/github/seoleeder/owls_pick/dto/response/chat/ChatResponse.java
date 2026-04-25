package io.github.seoleeder.owls_pick.dto.response.chat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Owls 챗봇 응답")
public record ChatResponse(
        @Schema(description = "현재 채팅 세션 ID", example = "1")
        Long sessionId,

        @Schema(description = "생성된 AI 응답", example = "Outer Wilds를 강력히 추천합니다. 이 게임은...")
        String reply
) {}