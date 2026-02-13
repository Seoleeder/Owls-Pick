package io.github.seoleeder.owls_pick.client.ITAD.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record ITADPriceResponse(
        String id,
        @JsonProperty("deals") List<Deal> deals
) {
    public record Deal (
            Shop shop,
            Price currentPrice,
            @JsonProperty("regular") OriginalPrice originalPrice,
            StoreLow storelow,
            Integer cut,
            @JsonProperty("expiry") LocalDateTime expiryDate,
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
