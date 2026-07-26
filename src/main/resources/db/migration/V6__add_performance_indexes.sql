-- ===========================================================================
-- V6 성능 최적화 및 쿼리 효율화 인덱스
-- ===========================================================================

-- 1. 게임(game) 도메인 및 메인 픽 최적화 인덱스

-- [game] 출시 예정작 조회 및 출시 상태 필터링 인덱스
create index if not exists idx_game_first_release on game (first_release asc);

-- [store_detail] itad 가격 동기화 in 쿼리 최적화 복합 인덱스
create index if not exists idx_store_detail_game_store on store_detail (game_id, store_name);

-- [game_company] 게임별 개발사/배급사 정보 조회 인덱스
create index if not exists idx_game_company_game_id on game_company (game_id);

-- [review_stat] 인기순 정렬(total_review) 조회 최적화 인덱스
create index if not exists idx_review_stat_total_review on review_stat (total_review desc);

-- [review_stat] 숨겨진 명작 전용 복합 인덱스 (review_score + total_review)
create index if not exists idx_review_stat_hidden_masterpiece on review_stat (review_score desc, total_review desc);

-- [review_stat] 트렌딩 픽 전용 부분 인덱스 (weekly_review > 0 조건절)
create index if not exists idx_review_stat_trending on review_stat (weekly_review desc, total_review desc) where weekly_review > 0;

-- [playtime] 퀵 플레이 조회 전용 인덱스 (main_story)
create index if not exists idx_playtime_main_story on playtime (main_story asc);


-- 2. 사용자(users) 도메인 최적화 인덱스

-- [wishlist] 게임별 찜 목록 조회 및 집계 인덱스
create index if not exists idx_wishlist_game_id on wishlist (game_id);