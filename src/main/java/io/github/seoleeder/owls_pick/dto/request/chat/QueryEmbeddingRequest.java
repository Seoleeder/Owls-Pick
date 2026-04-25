package io.github.seoleeder.owls_pick.dto.request.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "사용자 발화의 벡터 임베딩 요청 DTO")
public record QueryEmbeddingRequest(
        @Schema(description = "의도 파악을 위한 과거 대화 내역")
        List<ChatHistoryDto> history,

        @Schema(description = "벡터로 변환할 사용자 메시지 텍스트")
        String userMessage
) {}
