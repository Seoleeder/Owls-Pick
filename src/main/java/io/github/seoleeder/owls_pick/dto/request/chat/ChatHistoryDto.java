package io.github.seoleeder.owls_pick.dto.request.chat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대화 내역 전달용 단일 메시지 DTO")
public record ChatHistoryDto(
        @Schema(description = "역할 (user 또는 model)", example = "user")
        String role,

        @Schema(description = "메시지 내용", example = "우주 탐험 게임 찾아줘")
        String content
) {}
