package io.github.seoleeder.owls_pick.entity.user;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode // 복합키 식별을 위해 필수
public class WishlistId implements Serializable {

    private Long userId;
    private Long gameId;
}
