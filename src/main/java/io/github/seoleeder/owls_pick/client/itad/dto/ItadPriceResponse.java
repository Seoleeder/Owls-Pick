package io.github.seoleeder.owls_pick.client.itad.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record ItadPriceResponse(
        String id,
        @JsonProperty("deals") List<Deal> deals
) {
    public record Deal (
            Shop shop,
            @JsonProperty("price") Price currentPrice,
            @JsonProperty("regular") OriginalPrice originalPrice,
            @JsonProperty("storeLow") StoreLow storeLow,
            Integer cut,
            @JsonProperty("expiry") OffsetDateTime expiryDate,
            String url
    ){
        public record Shop(
                String id,
                String name
        ){}

        public record Price(
                Integer amountInt
        ){}

        public record OriginalPrice(
                Integer amountInt
        ){}

        public record StoreLow(
                Integer amountInt
        ){}
    }
}
