package io.github.seoleeder.owls_pick.entity.genai.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum GenaiFailReason {
    INSUFFICIENT_DATA(false),        // 유효 데이터 부족
    SAFETY_FILTER_REJECTED(false),   // 모델 안전 필터 거부
    NETWORK_ERROR(true),            // API 통신 장애
    INVALID_RESPONSE(false),         // 비정상 응답 포맷
    UNKNOWN_ERROR(true);             // 기타 예외

    private final boolean retryable;

    GenaiFailReason(boolean retryable) {
        this.retryable = retryable;
    }

    /**
     * 재시도 가능한 실패 사유 목록 반환
     */
    public static List<GenaiFailReason> getRetryableReasons() {
        return Arrays.stream(values())
                .filter(GenaiFailReason::isRetryable)
                .toList();
    }

}