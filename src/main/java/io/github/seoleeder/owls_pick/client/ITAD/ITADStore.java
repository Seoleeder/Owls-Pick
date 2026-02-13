package io.github.seoleeder.owls_pick.client.ITAD;

import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail.StoreName;

@Getter
public enum ITADStore {
    BLIZZARD("Blizzard", 4, StoreName.BLIZZARD),

    FANATICAL("Fanatical", 6, StoreName.FANATICAL), // Fanatical ID는 보통 15입니다

    EPIC_GAMES("Epic Game Store", 16, StoreName.EPIC_GAMES_STORE),

    GAMERS_GATE("GamersGate", 24, StoreName.GAMERSGATE),

    GREEN_MAN_GAMING("GreenManGaming", 36, StoreName.GREEN_MAN_GAMING),

    MICROSOFT_STORE("Microsoft Store", 48, StoreName.MICROSOFT_STORE),

    EA_STORE("EA Store", 52, StoreName.EA_STORE),

    STEAM("Steam", 61, StoreName.STEAM),

    UBISOFT("Ubisoft Store", 62, StoreName.UBISOFT_STORE),

    // 매핑되지 않는 스토어 처리용
    UNKNOWN("Unknown", -1, null);

    private final String itadName;
    private final int itadId;
    private final StoreName storeName;

    // 지원할 스토어 ID를 쉼표로 연결 (API 요청용)
    public static final String ALL_STORE_IDS = Arrays.stream(ITADStore.values())
            .filter(store -> store.getStoreName() != null)
            .map(store -> String.valueOf(store.getItadId()))
            .collect(Collectors.joining(","));

    ITADStore(String itadName, int itadId, StoreName storeName) {
        this.itadName = itadName;
        this.itadId = itadId;
        this.storeName = storeName;
    }

    // ID로 ITADStore 찾기
    public static ITADStore fromId(int itadId) {
        for (ITADStore store : values()) {
            if (store.itadId == itadId) return store;
        }
        return null;
    }
}
