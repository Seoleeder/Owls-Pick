package io.github.seoleeder.owls_pick.dto.embedding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "벡터 임베딩 배치 요청 DTO")
public record EmbeddingBatchRequest(
        @Valid
        @NotEmpty(message = "Game list for embedding must not be empty")
        @Schema(description = "임베딩을 요청할 게임 데이터 목록")
        List<GameEmbeddingData> games
) {
    @Schema(description = "원본 게임 메타데이터")
    public record GameEmbeddingData(
            @NotNull(message = "Game ID must not be null")
            @Schema(description = "게임 고유 ID", example = "12345")
            Long gameId,

            @NotBlank(message = "Title must not be blank")
            @Schema(description = "게임 원본 제목", example = "Outer Wilds")
            String title,

            // 한글 데이터는 존재할 경우에만 제공
            @Schema(description = "게임 설명 (한글 우선, 없으면 영문)", example = "우주를 탐험하며 타임 루프의 비밀을 푸는 게임입니다.")
            String description,

            @NotEmpty(message = "Genres must not be empty")
            @Schema(description = "영문 장르 목록", example = "[\"Adventure\", \"Indie\"]")
            List<String> genres,

            @NotEmpty(message = "Themes must not be empty")
            @Schema(description = "영문 테마 목록", example = "[\"Sci-fi\", \"Space\", \"Exploration\"]")
            List<String> themes,

            @Schema(description = "키워드 목록 (한글 우선, 없으면 영문)", example = "[\"우주\", \"타임루프\", \"퍼즐\"]")
            List<String> keywords,

            @Schema(description = "메인 스토리 플레이타임", example = "15")
            Integer mainStory
    ) {
            /**
             * 내부 소스 DTO를 통신용 데이터로 변환하는 정적 메서드
             */
            public static GameEmbeddingData from(EmbeddingSourceDto source){
                    return new GameEmbeddingData(
                            source.gameId(),
                            source.title(),
                            source.description(),
                            source.genres(),
                            source.themes(),
                            source.keywords(),
                            source.mainStory()
                    );
            }
    }
}
