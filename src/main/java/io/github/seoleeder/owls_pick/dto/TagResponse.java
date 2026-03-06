package io.github.seoleeder.owls_pick.dto;

import io.github.seoleeder.owls_pick.entity.game.enums.TagType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "태그(장르/테마) 응답 DTO")
public record TagResponse(
        @Schema(description = "태그 식별 코드 (Enum name)", example = "RPG")
        String code,

        @Schema(description = "한글 태그 타입", example = "롤플레잉")
        String korName,

        @Schema(description = "영문 태그 타입", example = "Role-playing (RPG)")
        String engName
) {
        // Tag enum을 DTO로 변환
        public static TagResponse from(TagType tagType) {
                return TagResponse.builder()
                        .code(tagType.name())
                        .korName(tagType.getKorName())
                        .engName(tagType.getEngName())
                        .build();
        }
}
