package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "review_stat")
public class ReviewStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_stat_game_id"))
    private Game game;

    @Column(name = "review_score")
    private int reviewScore;

    @Column(name = "review_score_desc",length = 30)
    private String reviewScoreDesc;

    @Column(name = "total_review")
    private int totalReview;

    @Column(name = "total_positive")
    private int totalPositive;

    @Column(name = "total_negative")
    private int totalNegative;

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

    public void updateStats(int reviewScore, String reviewScoreDesc,int totalReview, int totalPositive, int totalNegative) {
        this.reviewScore = reviewScore;
        this.reviewScoreDesc = reviewScoreDesc;
        this.totalReview = totalReview;
        this.totalPositive = totalPositive;
        this.totalNegative = totalNegative;
    }
}
