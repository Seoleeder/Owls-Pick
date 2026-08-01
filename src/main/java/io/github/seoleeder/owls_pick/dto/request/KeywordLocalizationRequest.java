package io.github.seoleeder.owls_pick.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "키워드 한글화 요청 DTO")
public record KeywordLocalizationRequest(

        @Schema(description = "한글화할 영문 키워드 목록")
        @NotEmpty(message = "Keywords list cannot be empty.")
        List<String> keywords
) {}
