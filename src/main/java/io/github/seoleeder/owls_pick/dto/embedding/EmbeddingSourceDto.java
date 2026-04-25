package io.github.seoleeder.owls_pick.dto.embedding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "임베딩 대상 게임 원본 데이터 DTO")
public record EmbeddingSourceDto(
        @NotNull(message = "Game ID must not be null")
        @Schema(description = "게임 고유 ID", example = "12345")
        Long gameId,

        @NotBlank(message = "Title must not be blank")
        @Schema(description = "게임 원본 제목", example = "Outer Wilds")
        String title,

        @NotBlank(message = "Description must not be blank")
        @Schema(description = "게임 설명", example = "우주를 탐험하며 타임 루프의 비밀을 푸는 게임입니다.")
        String description,

        @NotEmpty(message = "Genres must not be empty")
        @Schema(description = "영문 장르 목록")
        List<String> genres,

        @NotEmpty(message = "Themes must not be empty")
        @Schema(description = "영문 테마 목록")
        List<String> themes,
        @Schema(description = "키워드 목록 (Fallback 적용 완료)")
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
    public static EmbeddingSourceDto of(
            Long gameId,
            String title,
            String descEng,
            String descKo,
            List<String> genres,
            List<String> themes,
            List<String> keywordsEng,
            List<String> keywordsKo,
            Integer mainStory,
            String reviewScoreDesc,
            String reviewSummary
    ) {
        return new EmbeddingSourceDto(
                gameId, title,
                (descKo != null && !descKo.trim().isEmpty()) ? descKo : descEng,
                genres, themes,
                (keywordsKo != null && !keywordsKo.isEmpty()) ? keywordsKo : keywordsEng,
                mainStory, reviewScoreDesc, reviewSummary
        );
    }

    /**
     * 검색 및 답변 생성에 사용할 최종 텍스트 형식 반환
     */
    public String toFinalSourceText() {
        String formattedKeywords = (keywords != null && !keywords.isEmpty()) ? String.join(", ", keywords) : "None";
        String formattedPlaytime = (mainStory != null) ? mainStory + " hours" : "Unknown";

        return """
               Game Title: %s
               Description: %s
               Genres: %s
               Themes: %s
               Keywords: %s
               Playtime: %s
               Rating: %s
               Review Summary: %s
               """.formatted(
                title,
                description,
                String.join(", ", genres),
                String.join(", ", themes),
                formattedKeywords,
                formattedPlaytime,
                reviewScoreDesc,
                reviewSummary
        ).strip();
    }
}