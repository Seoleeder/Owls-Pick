package io.github.seoleeder.owls_pick.client.steam.policy;

public record SteamReviewCollectionPolicy(
        int targetQuota,
        int minVotesUp,
        int minLength
) {
    /**
     * 총 리뷰 수에 기반한 적응형 샘플링 및 필터링 정책 반환
     */
    public static SteamReviewCollectionPolicy of(int totalReviews) {
        if (totalReviews < 100) {
            // 악성 리뷰 방지용 최소 필터만 적용하여 전체 수집
            return new SteamReviewCollectionPolicy(totalReviews, 1, 15);

        } else if (totalReviews < 500) {
            // 제한적 샘플링 및 기본 유용함 검증
            return new SteamReviewCollectionPolicy(100, 3, 20);

        } else if (totalReviews < 3000) {
            // 중간 규모 모수 확보 및 검증 기준 강화
            return new SteamReviewCollectionPolicy(150, 5, 25);

        } else if (totalReviews < 10000) {
            // 대규모 모수 제한 및 높은 유용함 요구
            return new SteamReviewCollectionPolicy(200, 10, 30);

        } else {
            // 최상위 품질 리뷰 선별을 위한 임계치 대폭 상향
            return new SteamReviewCollectionPolicy(300, 15, 35);
        }
    }
}
