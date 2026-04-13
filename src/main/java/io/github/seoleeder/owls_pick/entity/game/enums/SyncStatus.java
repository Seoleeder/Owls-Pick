package io.github.seoleeder.owls_pick.entity.game.enums;

public enum SyncStatus {
    UNSYNCED,    // 동기화 전
    SUCCESS,    // 동기화 완료
    NO_DATA,    // 게임은 찾았으나 플레이타임 데이터가 아직 없음
    NOT_FOUND,  // HLTB 미등록 게임 (영구 스킵 대상)
    FAILED      // 타임아웃, 파싱 에러 등 일시적 오류 (재시도 대상)
}
