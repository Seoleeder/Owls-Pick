package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor (access = AccessLevel.PROTECTED)
@Entity
@Table
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "recommendation_id", nullable = false)
    private long recommendationId;

    @Column(name = "review_text", columnDefinition = "text", nullable = false)
    private String reviewText;

    @Column(name = "weighted_vote_score",precision = 5,scale = 2)
    private BigDecimal weightedVoteScore;

    @Column(name = "playtime_at_review",precision = 10, scale = 2)
    private BigDecimal playtimeAtReview;

    @Column(name = "voted_up")
    private Boolean votedUp;

    @Column(name = "votes_up")
    private int votesUp;

    @Column(name = "written_at")
    private LocalDateTime writtenAt;

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
