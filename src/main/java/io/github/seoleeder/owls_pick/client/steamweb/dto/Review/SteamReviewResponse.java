package io.github.seoleeder.owls_pick.client.steamweb.dto.Review;

import java.util.List;

public record SteamReviewResponse(
        SteamReviewStatsResponse.SteamReviewStats stats,
        List<SteamReviewDetailResponse.SteamReviewDetail> reviews
) {
}
