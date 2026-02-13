package io.github.seoleeder.owls_pick.client.steamweb.dto.Review;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record SteamReviewDetailResponse(
        List<SteamReviewDetail> reviews,
        String cursor
) {
    public record SteamReviewDetail(
            @JsonProperty("recommendation_id") Long recommendationId,
            Author author,
            @JsonProperty("weighted_vote_score") BigDecimal weightedVoteScore,
            @JsonProperty("review") String reviewText,
            @JsonProperty("votes_up") int votesUp,
            @JsonProperty("voted_up") boolean votedUp,
            @JsonProperty("timestamp_created") Long writtenAt
    ) {
        public record Author(
                @JsonProperty("playtime_at_review") int playtimeAtReview
        ) {}
    }
}
