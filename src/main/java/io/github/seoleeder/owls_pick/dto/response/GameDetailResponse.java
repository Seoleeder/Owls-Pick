package io.github.seoleeder.owls_pick.dto.response;

import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "게임 상세 정보 통합 응답 DTO")
public record GameDetailResponse(
        // 기본 게임 정보 (game 엔티티 컬럼)

        @Schema(description = "게임 ID", example = "190264")
        Long gameId,

        @Schema(description = "게임 타이틀 (영문)", example = "Red Dead Redemption 2")
        String title,

        @Schema(description = "게임 타이틀 (한글)", example = "레드 데드 리뎀션 2")
        String titleLocalization,

        @Schema(description = "게임 설명", example = "레드 데드 리뎀션 2는 도망자 신세가 된 무법자 아서 모건과 반 더 린드 갱단의 이야기를 다루고 있습니다.")
        String description,

        @Schema(description = "게임 스토리라인", example = "몰락해가는 서부 시대의 끝자락에서, 아서 모건이 추격과 내부 분열에 맞서 생존과 신념 사이의 위태로운 선택을 내리는 여정입니다.")
        String storyline,

        @Schema(description = "최초 출시일", example = "2019-12-06")
        LocalDate firstRelease,

        @Schema(description = "커버 이미지 ID (URL 조합용)", example = "co1q1f")
        String coverId,

        @Schema(description = "한국 등급 분류", example = "청소년 이용불가")
        String ratingKr,

        @Schema(description = "ESRB 등급 분류", example = "M (Mature)")
        String ratingEsrb,

        @Schema(description = "성인용 게임 여부", example = "true")
        Boolean isAdult,

        @Schema(description = "게임 모드", example = "[싱글, 멀티, 코옵]")
        List<String> mode,

        @Schema(description = "시점", example = "[1인칭, 3인칭]")
        List<String> perspective,

        @Schema(description = "AI 요약 리뷰", example = "압도적인 디테일과 살아있는 오픈월드의 정점")
        String reviewSummary,

        @Schema(description = "IGDB 기준 기대치/관심도 지표", example = "257")
        Integer hypes,

        // 1:1 매핑 정보
        @Schema(description = "태그 (장르, 테마, 키워드)")
        TagInfo tags,

        @Schema(description = "플레이타임 (단위: 시간)")
        PlaytimeInfo playtime,

        @Schema(description = "스팀 리뷰 통계")
        ReviewStatsInfo reviewStats,

        // 1:N 매핑 리스트 정보
        @Schema(description = "스토어별 가격 및 할인 정보")
        List<StorePriceInfo> stores,

        @Schema(description = "지원 언어 정보")
        List<LanguageSupportInfo> languages,

        @Schema(description = "개발사 및 퍼블리셔 정보")
        List<CompanyInfo> companies,

        @Schema(description = "스크린샷 이미지 목록")
        List<ScreenshotInfo> screenshots,

        // 위시리스트 정보
        @Schema(description = "유저 상호작용 (위시리스트) 정보")
        WishlistInfo wishlist

) {

    @Builder
    @Schema(description = "태그 정보 (장르, 테마, 키워드)")
    public record TagInfo(
            @Schema(description = "장르 목록", example = "[어드벤처, RPG]")
            List<String> genres,
            @Schema(description = "테마 목록", example = "[액션, 드라마, 오픈월드]")
            List<String> themes,
            @Schema(description = "핵심 키워드 목록", example = "[승마, 낚시, 피]")
            List<String> keywords
    ) {}

    @Builder
    @Schema(description = "스토어별 가격 및 할인 정보")
    public record StorePriceInfo(
            @Schema(description = "스토어 이름", example = "Steam")
            StoreDetail.StoreName name,

            @Schema(description = "스토어 바로가기 URL", example = "https://store.steampowered.com/app/190264")
            String url,

            @Schema(description = "정가 (원)", example = "64800")
            Integer originalPrice,

            @Schema(description = "할인가 (원)", example = "45360")
            Integer discountPrice,

            @Schema(description = "할인율 (%)", example = "30")
            Integer discountRate,

            @Schema(description = "할인 종료 일시", example = "2026-04-01T23:59:59")
            LocalDateTime expiryDate
    ) {}

    @Builder
    @Schema(description = "지원 언어 정보")
    public record LanguageSupportInfo(
            @Schema(description = "언어명", example = "Korean")
            String language,

            @Schema(description = "음성 지원 여부", example = "false")
            Boolean voiceSupport,

            @Schema(description = "자막 지원 여부", example = "true")
            Boolean subtitle,

            @Schema(description = "인터페이스(UI) 지원 여부", example = "true")
            Boolean interfaceSupport
    ) {}

    @Builder
    @Schema(description = "개발사 및 퍼블리셔 정보")
    public record CompanyInfo(
            @Schema(description = "회사명", example = "FromSoftware")
            String name,

            @Schema(description = "회사 로고 이미지 ID", example = "logo_fromsoftware")
            String logo,

            @Schema(description = "개발사 여부", example = "true")
            Boolean isDeveloper,

            @Schema(description = "퍼블리셔 여부", example = "false")
            Boolean isPublisher
    ) {}

    @Builder
    @Schema(description = "스크린샷 정보")
    public record ScreenshotInfo(
            @Schema(description = "이미지 고유 ID (URL 조립용)", example = "scm8ru")
            String imageId,

            @Schema(description = "원본 가로 해상도", example = "1920")
            Integer width,

            @Schema(description = "원본 세로 해상도", example = "1080")
            Integer height
    ) {}

    @Builder
    @Schema(description = "플레이타임 정보 ")
    public record PlaytimeInfo(
            @Schema(description = "메인 스토리", example = "58")
            Integer mainStory,

            @Schema(description = "메인 + 서브 퀘스트", example = "102")
            Integer mainExtras,

            @Schema(description = "완벽 클리어 (도전과제 100%)", example = "135")
            Integer completionist
    ) {}

    @Builder
    @Schema(description = "리뷰 통계 정보")
    public record ReviewStatsInfo(
            @Schema(description = "스팀 리뷰 스코어", example = "9")
            Integer reviewScore,

            @Schema(description = "스코어 설명", example = "압도적으로 긍정적")
            String reviewScoreDesc,

            @Schema(description = "긍정적 리뷰 수", example = "45000")
            Integer totalPositive,

            @Schema(description = "부정적 리뷰 수", example = "3200")
            Integer totalNegative,

            @Schema(description = "총 리뷰 수", example = "48200")
            Integer totalReview,

            @Schema(description = "AI 리뷰 요약", example = "압도적인 볼륨과 훌륭한 레벨 디자인을 갖춘 명작...")
            String reviewSummary
    ) {}

    @Builder
    @Schema(description = "위시리스트 정보")
    public record WishlistInfo(
            @Schema(description = "현재 로그인한 유저의 위시리스트 추가 여부", example = "true")
            boolean isWished,

            @Schema(description = "이 게임이 위시리스트에 담긴 총 횟수", example = "12500")
            long totalWishCount
    ) {}
}