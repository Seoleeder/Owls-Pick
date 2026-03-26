package io.github.seoleeder.owls_pick.dto.response;

/**
 * FastAPI로부터 반환받는 키워드 한글화 응답 DTO
 */
public record KeywordLocalizationResponse(
        String engName,
        String korName
) {
}
