package io.github.seoleeder.owls_pick.entity.game.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;


/**
 * 성인 (19금) 콘텐츠 필터링을 위한 키워드 목록
 * */
@Getter
@RequiredArgsConstructor
public enum AdultKeyword {

    // --- 선정성 (Sexual) ---
    NUDITY("nudity"),
    SEXUAL_CONTENT("sexual content"),
    HIGH_SEXUAL_CONTENT("high sexual content"),
    SEXUAL_THEMES("sexual themes"),
    ADULT("adult"),
    ADULT_CONTENT("adult content"),
    SEX_WORK("sex work"),
    SEXUAL_VIOLENCE("sexual violence"),
    HENTAI("hentai"),
    HENTAI_TALES("hentai tales"),
    EROTICA("erotica"),
    BONDAGE("bondage"),

    // --- 패치 및 DLC (Technical Adult) ---
    NSFW("nsfw"),
    NSFW_PATCH("nsfw_patch"),
    NSFW_DLC("nsfw_dlc"),
    NSFW_VERSION_EXISTS("nsfw version exists"),

    // --- 잔혹성 (Violence/Gore) ---
    GORE("gore"),
    EXTREME_VIOLENCE("extreme violence");

    private final String keywordName;

    public static List<String> getAllNames() {
        return Arrays.stream(values())
                .map(AdultKeyword::getKeywordName)
                .toList();
    }
}
