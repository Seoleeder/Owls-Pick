package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table
public class Tag {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tag_game_id"))
    private Game game;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> genres = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> themes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> keywords = new ArrayList<>();

}
