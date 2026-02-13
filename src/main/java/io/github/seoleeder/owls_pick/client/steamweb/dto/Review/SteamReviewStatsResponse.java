package io.github.seoleeder.owls_pick.client.steamweb.dto.Review;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SteamReviewStatsResponse(
        @JsonProperty("query_summary") SteamReviewStats querySummary) {
    public record SteamReviewStats(
            @JsonProperty("review_score") int reviewScore,
            @JsonProperty("review_score_desc") String reviewScoreDesc,
            @JsonProperty("total_reviews") int totalReview,
            @JsonProperty("total_positive") int totalPositive,
            @JsonProperty("total_negative") int totalNegative
    ) {}
}
