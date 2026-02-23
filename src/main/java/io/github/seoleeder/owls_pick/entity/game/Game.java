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

    @Column(unique = true)
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

    @Column
    private String description;

    @Column
    private String storyline;

    @Column
    private LocalDate firstRelease;

    @Column(length = 30)
    private String ratingKr;

    @Column(length = 30)
    private String ratingEsrb;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> mode = new ArrayList<>();;

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


}
