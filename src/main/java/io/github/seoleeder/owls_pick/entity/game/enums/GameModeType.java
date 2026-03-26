package io.github.seoleeder.owls_pick.entity.game.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum GameModeType {

    BATTLE_ROYALE("배틀로얄", "Battle Royale"),
    CO_OPERATIVE("CO-OP", "Co-operative"),
    MMO("MMO", "Massively Multiplayer Online (MMO)"),
    MULTIPLAYER("멀티 플레이어", "Multiplayer"),
    SINGLE_PLAYER("싱글 플레이어", "Single player"),
    SPLIT_SCREEN("화면 분할", "Split screen"),

    // 신규 데이터가 들어올 경우 폴백
    UNKNOWN("기타", "Unknown");

    private final String korName;
    private final String engName;

    /**
     * 영문 명칭을 기반으로 ENUM 객체를 반환
     * 일치하는 값이 없으면 예외를 던지지 않고 UNKNOWN을 반환
     */
    public static GameModeType fromEngName(String engName) {
        return Arrays.stream(values())
                .filter(mode -> mode.getEngName().equalsIgnoreCase(engName))
                .findFirst()
                .orElse(UNKNOWN);
    }
}