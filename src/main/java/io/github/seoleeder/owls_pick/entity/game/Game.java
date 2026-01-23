package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private int id;

    @Column(name = "igdb_id", nullable = false, unique = true)
    private int igdbId;

    @Column(name = "itnd_id", nullable = false, unique = true)
    private int itnd_id;

    @Column(nullable = false)
    private String title;

    @Column(name = "title_localization", nullable = false)
    private String titleLocalization;

    @Column(length = 30, nullable = false)
    private String type;

    @Column(name = "release_status", nullable = false)
    private String releaseStatus;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> platform;

    @Column
    private String description;

    @Column
    private String storyline;

    @Column(name = "first_release", nullable = false)
    private LocalDate firstRelease;

    @Column(name = "rating_kr", length = 30)
    private String ratingKr;

    @Column(name = "rating_esrb", length = 30)
    private String ratingEsrb;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> mode;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> perspective;

    @Column(name = "cover_id", length = 30)
    private String coverId;

    @Column
    private int hypes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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

}
