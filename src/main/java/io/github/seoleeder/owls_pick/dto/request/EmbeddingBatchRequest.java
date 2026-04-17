package io.github.seoleeder.owls_pick.dto.request;

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
        List<RawGameData> games
) {
    @Schema(description = "원본 게임 메타데이터")
    public record RawGameData(
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
            Integer mainStory,

            @NotBlank(message = "Review score description must not be blank")
            @Schema(description = "스팀 평가 등급", example = "Overwhelmingly Positive")
            String reviewScoreDesc,

            @NotBlank(message = "Review summary must not be blank")
            @Schema(description = "리뷰 요약본", example = "A masterpiece of exploration with mind-bending mechanics.")
            String reviewSummary
    ) {

            public static RawGameData of(
                    Long gameId, String title,
                    String descEng, String descKo,
                    List<String> genres, List<String> themes,
                    List<String> keywordsEng, List<String> keywordsKo,
                    Integer mainStory, String reviewScoreDesc, String reviewSummary
            ) {
                    return new RawGameData(
                            gameId,
                            title,
                            // 한글 설명이 비어있지 않으면 한글 사용, 그 외 영문 사용
                            (descKo != null && !descKo.trim().isEmpty()) ? descKo : descEng,
                            genres,
                            themes,
                            // 한글 키워드 배열이 존재하면 한글 사용, 그 외 영문 사용
                            (keywordsKo != null && !keywordsKo.isEmpty()) ? keywordsKo : keywordsEng,
                            mainStory,
                            reviewScoreDesc,
                            reviewSummary
                    );
            }
    }
}
