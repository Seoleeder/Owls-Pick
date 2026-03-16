package io.github.seoleeder.owls_pick.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DiscountNotificationResponse(
        Long id,
        String title,
        String gameTitle,
        int discountRate,
        boolean isRead,
        LocalDateTime expiryDate,
        String createdAt
) {}
