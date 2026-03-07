package io.github.seoleeder.owls_pick.dto.section;

import io.github.seoleeder.owls_pick.dto.UpcomingGameResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor
@Schema(description = "출시 예정작 섹션 응답 래퍼 DTO")
public record UpcomingSectionResponse(
        @Schema(description = "섹션 타이틀", example = "출시 예정 최고 기대작")
        String titleKeyword,

        @Schema(description = "출시 예정 게임 리스트 (페이징)")
        Page<UpcomingGameResponse> games
) {
}
