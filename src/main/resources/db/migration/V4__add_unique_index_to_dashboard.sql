CREATE UNIQUE INDEX uk_dashboard_type_date_game
    ON dashboard (curation_type, reference_at, game_id);