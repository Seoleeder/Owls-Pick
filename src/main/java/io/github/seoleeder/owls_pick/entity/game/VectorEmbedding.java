package io.github.seoleeder.owls_pick.entity.game;

import io.github.seoleeder.owls_pick.entity.game.enums.status.EmbeddingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vector_embedding")
public class VectorEmbedding {
    @Id
    private Long gameId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(768)")
    private float[] embedding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmbeddingStatus embeddingStatus = EmbeddingStatus.UNEMBEDDED;

    // RAG용 프롬프트 원문
    @Column(columnDefinition = "TEXT", nullable = false)
    private String sourceText;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 임베딩 데이터 갱신
     */
    public void updateEmbeddingData(float[] embedding, EmbeddingStatus status, String sourceText) {
        this.embedding = embedding;
        this.sourceText = sourceText;
        this.embeddingStatus = status;
    }
}
