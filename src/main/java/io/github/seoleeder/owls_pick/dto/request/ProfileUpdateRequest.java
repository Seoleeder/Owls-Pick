package io.github.seoleeder.owls_pick.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "마이 페이지 데이터 수정 요청 DTO (변경할 필드만 전송)")
public record ProfileUpdateRequest(

    @Schema(description = "변경할 닉네임", example = "메이더")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
    String nickname,

    @Schema(description = "변경할 선호 태그 목록", example = "[FPS, Sports]")
    @Size(min = 3, message = " 최소 3개 이상의 태그를 선택해주세요.")
    List<String> preferredTags,

    @Schema(description = "변경할 선호 스토어 목록", example = "[Steam]")
    List<String> preferredStores

){}
