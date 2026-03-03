package io.github.seoleeder.owls_pick.entity.game.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ThemeType implements TagType {

    // 메인 그리드 노출 (핵심 Top 5)
    ACTION("액션", "Action", true, false),
    FANTASY("판타지", "Fantasy", true,false),
    HORROR("공포/호러", "Horror", true,false),
    SF("SF/우주", "Science fiction", true,false),
    SURVIVAL("생존", "Survival", true,false),

    // 더보기용
    EROTIC("성인", "Erotic", false,true),
    COMEDY("코미디", "Comedy", false,false),
    MYSTERY("미스터리", "Mystery", false,false),
    ROMANCE("로맨스", "Romance", false,false),
    OPEN_WORLD("오픈월드", "Open world", false,false),
    SANDBOX("샌드박스", "Sandbox", false,false),
    HISTORICAL("역사", "Historical", false,false),
    PARTY("파티", "Party", false,false),
    DRAMA("드라마", "Drama", false,false),
    THRILLER("스릴러", "Thriller", false,false),
    WARFARE("전쟁/밀리터리", "Warfare", false,false),
    KIDS("아동/가족", "Kids", false,false),
    BUSINESS("비즈니스/경영", "Business", false,false),
    EDUCATIONAL("교육", "Educational", false,false),
    STEALTH("잠입", "Stealth", false,false);

    private final String korName;
    private final String engName;
    private final boolean isPopular;
    private final boolean isAdult;

    public static List<ThemeType> getPopular() {
        return Arrays.stream(values())
                .filter(t -> t.isPopular)
                .toList();
    }
}
