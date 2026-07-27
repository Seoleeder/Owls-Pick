package io.github.seoleeder.owls_pick.repository.dto;

import java.util.List;

/**
 * 메인 픽 스케줄러 태그 조합 집계용 DTO
 */
public record TagArrayDto(
        List<String> genres,
        List<String> themes
) {}
