package io.github.seoleeder.owls_pick.client.IGDB.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IGDBGameSummaryResponse(
        @JsonProperty("id") Long igdbId,
        @JsonProperty("external_games") List <ExternalApp> externalApps,
        @JsonProperty("game_localizations") List<TitleLocalization> titleLocalization,
        @JsonProperty("game_type") Type type,
        @JsonProperty("game_status") GameStatus gameStatus,
        @JsonProperty("platforms") List<Platform> platforms,
        @JsonProperty("summary") String description,
        @JsonProperty("storyline") String storyline,
        @JsonProperty("first_release_date") Long first_release,
        @JsonProperty("updated_at") Long updatedAt,
        @JsonProperty("age_ratings") List<AgeRating> ageRatings,
        @JsonProperty("game_modes") List<GameMode> modes,
        @JsonProperty("player_perspectives") List <Perspective> perspectives,
        @JsonProperty("cover") Cover cover,
        @JsonProperty("hypes") int hypes


) {

    //해당 게임의 스토어 내 앱 ID 추출
    public record ExternalApp(
            @JsonProperty("id") Long storeAppid,
            @JsonProperty("external_game_source") int storeId
    ){}

    //title localization
    public record TitleLocalization (
            String name,
            Region region
    ){
        public record Region(
                Long id,
                String name
        ){}
    }

    public record Type (
            Long id,
            String type
    ){}

    public record GameStatus(
            String status
    ){}

    public record Platform(
            Long id,
            String name
    ){}

    public record AgeRating (
            Long id,
            @JsonProperty("organization") Organization organization,
            @JsonProperty("rating_category") RatingCategory ratingCategories
    ){
        public record Organization(
                Long id,
                String name
        ){}

        public record RatingCategory(
                Long id,
                String rating
        ){}
    }

    public record GameMode(
            Long id,
            String name
    ){}

    public record Perspective (
            Long id,
            String name
    ){}

    public record Cover(
            @JsonProperty("image_id") String imageId
    ){}
}
