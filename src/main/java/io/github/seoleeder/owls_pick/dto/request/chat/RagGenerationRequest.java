package io.github.seoleeder.owls_pick.dto.request.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Owls 최종 답변 생성 요청 DTO")
public record RagGenerationRequest(
        @Schema(description = "의도 파악을 위한 과거 대화 내역")
        List<ChatHistoryDto> history,

        @Schema(description = "사용자 원본  텍스트")
        String userMessage,

        @Schema(description = "DB에서 유사도 검색을 통해 추출한 연관 게임 데이터 텍스트 목록")
        List<String> contexts
) {}
