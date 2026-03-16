package io.github.seoleeder.owls_pick.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "알림 설정 토글 요청 DTO")
public record NotificationToggleRequest(
        @NotNull(message = "Discount notification setting is required.")
        @Schema(description = "할인 알림 수신 동의 여부 변경", example = "true")
        Boolean isDiscountNotificationEnabled
) {}
