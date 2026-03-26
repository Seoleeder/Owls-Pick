package io.github.seoleeder.owls_pick.dto.request;

import java.util.List;

/**
 * 한글화 엔진에 번역을 요청할 키워드 리스트 DTO
 */
public record KeywordLocalizationRequest(
        List<String> keywords
) {}
