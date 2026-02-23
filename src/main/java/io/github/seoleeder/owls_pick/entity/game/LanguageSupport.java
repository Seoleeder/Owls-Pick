package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="language_support")
public class LanguageSupport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_language_support_game_id"))
    private Game game;

    @Column(length = 30)
    private String language;

    @Column
    private Boolean voiceSupport;

    @Column
    private Boolean subtitle;

    @Column
    private Boolean interSupport;
}
