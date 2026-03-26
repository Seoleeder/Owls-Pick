package io.github.seoleeder.owls_pick.entity.game.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PerspectiveType {

    AUDITORY("오디오 기반", "Auditory"),
    BIRD_VIEW("쿼터뷰/탑다운", "Bird view / Isometric"),
    FIRST_PERSON("1인칭", "First person"),
    SIDE_VIEW("횡스크롤/사이드뷰", "Side view"),
    TEXT("텍스트 기반", "Text"),
    THIRD_PERSON("3인칭", "Third person"),
    VR("VR (가상현실)", "Virtual Reality"),

    // 안전 장치: 매칭되지 않는 신규 데이터가 들어올 경우 폴백
    UNKNOWN("기타", "Unknown");

    private final String korName;
    private final String engName;

    /**
     * 영문 명칭을 기반으로 ENUM 객체를 반환합니다.
     * 일치하는 값이 없으면 예외를 던지지 않고 UNKNOWN을 반환하여 시스템 안정성을 유지합니다.
     */
    public static PerspectiveType fromEngName(String engName) {
        return Arrays.stream(values())
                .filter(perspective -> perspective.getEngName().equalsIgnoreCase(engName))
                .findFirst()
                .orElse(UNKNOWN);
    }
}