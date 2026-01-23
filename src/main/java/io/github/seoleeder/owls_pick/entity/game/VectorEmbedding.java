package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(name = "vector_embedding")
public class VectorEmbedding {
    @Id
    private Integer gameId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(columnDefinition = "vector(768)")
    private float[] embedding;
}
