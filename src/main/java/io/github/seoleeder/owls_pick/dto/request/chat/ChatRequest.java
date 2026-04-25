package io.github.seoleeder.owls_pick.dto.request.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Owls 챗봇 사용자 발화 요청 DTO")
public record ChatRequest(
        @Schema(description = "채팅 세션 ID (새로운 대화일 경우 null)", example = "1")
        Long sessionId,

        @NotBlank(message = "User message must not be blank.")
        @Schema(description = "사용자 메시지 내용", example = "우주 탐험과 양자 역학을 다루는 게임 찾아줘")
        String userMessage
) { }
