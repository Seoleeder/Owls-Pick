package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode // 복합키 식별을 위해 필수
public class GameCompanyId implements Serializable {

    private Long gameId;
    private Long companyId;
}