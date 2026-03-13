package io.github.seoleeder.owls_pick.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 최초 로그인 시 온보딩 데이터 수집을 위한 DTO
 */
@Schema(description = "사용자 온보딩 정보 등록 요청 DTO")
public record OnboardingRequest(
        @Schema(description = "닉네임", example = "이찌")
        @NotNull(message = "Nickname is required.")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
        String nickname,

        @Schema(description = "생년월일", example = "1994-10-31")
        @NotNull(message = "Birth date is required.")
        LocalDate birthDate,

        @Schema(description = "선호 게임 태그 목록", example = "[\"Indie\", \"Action\", \"RPG\"]")
        @NotNull(message = "Preferred tags are required.")
        @Size(min = 3, message = "Please select at least 3 tags for personalized recommendations.")
        List<String> preferredTags,

        @Schema(description = "선호 게임 스토어 목록", example = "[\"Steam\", \"Epic Games Store\"]")
        @NotNull(message = "Preferred stores are required.")
        List<String> preferredStores
) {
}
