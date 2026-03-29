package io.github.seoleeder.owls_pick.client.itad.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

@Schema(description = "ITAD 벌크 ID 매핑 응답 객체")
public record ItadBulkResponse(

        @Schema(description = "스팀 App ID를 키(Key)로, ITAD UUID를 값(Value)으로 가지는 Map")
        @NotNull(message = "Response data map must not be null")
        @JsonAnySetter
        Map<String, String> rawData

) {
    public ItadBulkResponse {
        if (rawData == null) {
            rawData = new HashMap<>();
        }
    }

    public String getUuidBySteamId(String steamId) {
        return rawData.get("app/" + steamId);
    }
}