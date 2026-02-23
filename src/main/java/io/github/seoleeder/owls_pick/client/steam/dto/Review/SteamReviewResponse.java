package io.github.seoleeder.owls_pick.client.steam.dto.Review;

import java.util.List;

public record SteamReviewResponse(
        SteamReviewStatsResponse.SteamReviewStats stats,
        List<SteamReviewDetailResponse.SteamReviewDetail> reviews
) {
}
