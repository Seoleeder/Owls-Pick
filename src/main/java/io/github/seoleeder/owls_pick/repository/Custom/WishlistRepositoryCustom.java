package io.github.seoleeder.owls_pick.repository.Custom;


import io.github.seoleeder.owls_pick.repository.dto.WishlistQueryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WishlistRepositoryCustom {

    // 특정 유저의 위시리스트와 게임 데이터를 묶어서 페이징 조회
    Page<WishlistQueryDto> findWishlistPageByUserId(Long userId, Pageable pageable);
}
