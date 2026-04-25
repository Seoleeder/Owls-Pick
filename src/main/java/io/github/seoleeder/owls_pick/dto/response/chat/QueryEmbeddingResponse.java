package io.github.seoleeder.owls_pick.dto.response.chat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 질문에 대한 벡터 임베딩 결과 반환용 응답 DTO")
public record QueryEmbeddingResponse(
        @Schema(description = "사용자 질문의 임베딩 벡터 값")
        float[] vector
) {}
