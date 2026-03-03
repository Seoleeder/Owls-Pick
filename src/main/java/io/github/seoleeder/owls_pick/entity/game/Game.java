package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long igdbId;

    @Column
    private String itadId;

    @Column(nullable = false)
    private String title;

    @Column
    private String titleLocalization;

    @Column(length = 30)
    private String type;

    @Column
    private String releaseStatus;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> platform = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String storyline;

    @Column
    private LocalDate firstRelease;

    @Column(length = 30)
    private String ratingKr;

    @Column(length = 30)
    private String ratingEsrb;

    @Column (nullable = false)
    @Builder.Default
    private Boolean isAdult = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> mode = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> perspective = new ArrayList<>();

    @Column(length = 30)
    private String coverId;

    @Column
    private int hypes;

    @Column
    private LocalDateTime igdbUpdatedAt;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist // 저장 전 실행
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate // 수정 전 실행
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public void connectToIgdb(Long newIgdbId) {
        if (this.igdbId != null) {
            return;
        }
        this.igdbId = newIgdbId;
    }

    public void updateItadId(String itadId){
        if (this.itadId != null) {
            return;
        }
        this.itadId = itadId;
    }

    //IGDB Summary Date Update
    public void updateFromSummary(
            String titleLocalization,
            String type,
            String releaseStatus,
            List<String> platform,
            String description,
            String storyline,
            LocalDate firstRelease,
            int hypes,
            String coverId,
            String ratingKr,
            String ratingEsrb,
            List<String> mode,
            List<String> perspective,
            LocalDateTime igdbUpdatedAt
    ) {
        if(this.igdbId == null) {
            this.igdbId = igdbId;
        }

        if (titleLocalization != null && !titleLocalization.isBlank()) {
            this.titleLocalization = titleLocalization;
        }

        this.description = description;
        this.storyline = storyline;
        this.type = type;
        this.releaseStatus = releaseStatus;
        this.firstRelease = firstRelease;
        this.igdbUpdatedAt = igdbUpdatedAt;
        this.hypes = hypes;
        this.coverId = coverId;

        // 심의 등급 할당
        this.ratingEsrb = ratingEsrb;
        this.ratingKr = ratingKr;

        // 리스트 교체
        this.platform = platform;
        this.mode = mode;
        this.perspective = perspective;
    }

    /**
     *  심의 등급(국내 및 북미)과 태그 정보를 바탕으로 성인 콘텐츠 여부 판단 및 상태 갱신
     */
    public void evaluateAdultStatus(Tag tag) {
        // 1. 한국 심의(GRAC) 검사: 문자열에 "18" 또는 "19"가 포함되어 있는지 확인
        boolean isKrAdult = this.ratingKr != null &&
                (this.ratingKr.contains("18") || this.ratingKr.contains("19"));

        // 2. 북미 심의(ESRB) 검사: 대소문자 무시하고 "M" 또는 "AO" 인지 확인
        boolean isEsrbAdult = this.ratingEsrb != null &&
                (this.ratingEsrb.equalsIgnoreCase("M") || this.ratingEsrb.equalsIgnoreCase("AO"));

        boolean isAdultRating = isKrAdult || isEsrbAdult;
        // 2. 넘겨받은 태그가 19금인지 판단
        boolean hasAdultTag = (tag != null && tag.isAdult());

        // 3. 둘 중 하나라도 해당하면 내 상태를 true로 변경
        this.isAdult = isAdultRating || hasAdultTag;
    }
}
