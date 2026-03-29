package io.github.seoleeder.owls_pick.dto.response;

import java.util.List;

/**
 * FastAPI로부터 반환받는 키워드 한글화 리스트 DTO
 */
public record KeywordLocalizationBulkResponse(
        List<KeywordLocalizationResponse> localizationResults
) {
    public record KeywordLocalizationResponse(
            String engName,
            String korName
    ) {
    }
}
