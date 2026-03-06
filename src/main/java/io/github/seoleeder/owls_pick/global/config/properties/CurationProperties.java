package io.github.seoleeder.owls_pick.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owls-pick.curation")
public record CurationProperties(
        Intersection intersection,
        HiddenMasterpiece hiddenMasterpiece,
        Trending trending,
        ShortPlaytime shortPlaytime
) {
    public record Intersection(
            int minRequiredGames
    ) {}

    public record HiddenMasterpiece(
            int minReviews,
            int maxReviews,
            int minScore
    ) {}

    public record Trending(
            int daysRange,
            int minScore
    ) {}

    public record ShortPlaytime(
            int maxPlaytime,
            int minScore
    ) {}
}
