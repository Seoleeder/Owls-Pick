package io.github.seoleeder.owls_pick.entity.game.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ThemeType implements TagType {

    // 메인 그리드 노출 (핵심 Top 5)
    ACTION("액션", "Action", true),
    FANTASY("판타지", "Fantasy", true),
    HORROR("공포/호러", "Horror", true),
    SF("SF/우주", "Science fiction", true),
    SURVIVAL("생존", "Survival", true),

    // 더보기용
    EROTIC("성인", "Erotic", false),
    COMEDY("코미디", "Comedy", false),
    MYSTERY("미스터리", "Mystery", false),
    ROMANCE("로맨스", "Romance", false),
    OPEN_WORLD("오픈월드", "Open world", false),
    SANDBOX("샌드박스", "Sandbox", false),
    HISTORICAL("역사", "Historical", false),
    PARTY("파티", "Party", false),
    DRAMA("드라마", "Drama", false),
    THRILLER("스릴러", "Thriller", false),
    WARFARE("전쟁/밀리터리", "Warfare", false),
    KIDS("아동/가족", "Kids", false),
    BUSINESS("비즈니스/경영", "Business", false),
    EDUCATIONAL("교육", "Educational", false),
    STEALTH("잠입", "Stealth", false);

    private final String korName;
    private final String engName;
    private final boolean isPopular;

    public static List<ThemeType> getPopular() {
        return Arrays.stream(values())
                .filter(t -> t.isPopular) // Java 25: 필드 직접 접근
                .toList();
    }
}
