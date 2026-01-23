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
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "voice_support")
    private Boolean voiceSupport;

    @Column
    private Boolean subtitle;

    @Column(name = "inter_support")
    private Boolean interSupport;
}
