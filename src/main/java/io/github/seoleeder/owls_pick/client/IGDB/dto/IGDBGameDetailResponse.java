package io.github.seoleeder.owls_pick.client.IGDB.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IGDBGameDetailResponse(
        @JsonProperty("id") Long igdbId,
        @JsonProperty("external_games") List<ExternalApp> externalApps,
        @JsonProperty("genres") List<Genre> genres,
        @JsonProperty("themes") List<Theme> themes,
        @JsonProperty("keywords") List<Keyword> keywords,
        @JsonProperty("involved_companies") List<Company> companies,
        @JsonProperty("screenshots") List<Screenshot> screenshots,
        @JsonProperty("language_supports") List<LanguageSupport> languageSupports

) {
    //해당 게임의 스토어 내 앱 ID 추출
    public record ExternalApp(
            @JsonProperty("id") Long storeAppid,
            @JsonProperty("external_game_source") int storeAppId
    ){}

    public record Genre(
            Long id,
            String name
    ){}

    public record Theme(
            Long id,
            String name
    ){}

    public record Keyword(
            Long id,
            String name
    ){}

    public record Company(
            Long id,
            @JsonProperty("company") CompanyDetail companyDetail,
            @JsonProperty("developer") boolean isDeveloper,
            @JsonProperty("publisher") boolean isPublisher

    ){
        public record CompanyDetail(
                Long id,
                Logo logo,
                String name,
                List<Website> websites
        ){
            public record Logo(
                    @JsonProperty("image_id") String imageId
            ){}

            public record Website(
                    String url,
                    Integer type
            ){}
        }
    }

    public record Screenshot(
            int height,
            @JsonProperty("image_id") String imageId,
            int width
    ){}

    public record LanguageSupport(
            @JsonProperty("language") LanguageInfo languageInfo,
            @JsonProperty("language_support_type") SupportType supportType
    ){
        public record LanguageInfo(
                Long id,
                String name
        ){}
        public record SupportType(
                Long id,
                String name
        ){}

    }

}
