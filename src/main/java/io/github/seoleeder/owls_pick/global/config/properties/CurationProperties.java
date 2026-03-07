package io.github.seoleeder.owls_pick.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owls-pick.curation")
public record CurationProperties(
        Upcoming upcoming,
        Intersection intersection,
        HiddenMasterpiece hiddenMasterpiece,
        Trending trending,
        ShortPlaytime shortPlaytime
) {
    public record Upcoming(
            int minHypes,
            int periodMonths
    ){}
    public record Intersection(
            int minRequiredGames
    ) {}

    public record HiddenMasterpiece(
            int minReviews,
            int maxReviews,
            int minReviewScore
    ) {}

    public record Trending(
            int daysRange,
            int minReviewScore
    ) {}

    public record ShortPlaytime(
            int maxPlaytime,
            int minScore
    ) {}
}
