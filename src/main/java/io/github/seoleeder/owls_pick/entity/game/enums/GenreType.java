package io.github.seoleeder.owls_pick.entity.game.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum GenreType implements TagType {
    // 메인 그리드 노출
    INDIE("인디", "Indie", true),
    ADVENTURE("어드벤처", "Adventure", true),
    SIMULATOR("시뮬레이션", "Simulator", true),
    STRATEGY("전략", "Strategy", true),
    RPG("롤플레잉", "Role-playing (RPG)", true),

    // 더보기용 (isPopular = false)
    PUZZLE("퍼즐", "Puzzle", false),
    SPORT("스포츠", "Sport", false),
    PLATFORM("플랫포머", "Platform", false),
    RACING("레이싱", "Racing", false),
    ARCADE("아케이드", "Arcade", false),
    SHOOTER("슈팅", "Shooter", false),
    VISUAL_NOVEL("비주얼 노벨", "Visual Novel", false),
    POINT_AND_CLICK("포인트 앤 클릭", "Point-and-click", false),
    CARD_AND_BOARD("카드 & 보드게임", "Card & Board Game", false),
    TACTICAL("전술", "Tactical", false),
    TURN_BASED_STRATEGY("턴제 전략", "Turn-based strategy (TBS)", false),
    FIGHTING("격투", "Fighting", false),
    HACK_AND_SLASH("핵 앤 슬래시", "Hack and slash/Beat 'em up", false),
    MUSIC("음악", "Music", false),
    REAL_TIME_STRATEGY("실시간 전략", "Real Time Strategy (RTS)", false);

    private final String korName;
    private final String engName;
    private final boolean isPopular;

    public static List<GenreType> getPopular() {
        return Arrays.stream(values()).filter(g -> g.isPopular).toList();
    }
}

