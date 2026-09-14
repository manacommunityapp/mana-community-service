CREATE TABLE IF NOT EXISTS sports_race_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    team_id BIGINT,
    heat_number INT,
    lane_number INT,
    finish_time_millis BIGINT,
    formatted_time VARCHAR(50),
    race_status VARCHAR(20),
    overall_rank INT,
    heat_rank INT,
    personal_best_millis BIGINT,
    is_personal_best BOOLEAN DEFAULT FALSE,
    split_times TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_race_result_match FOREIGN KEY (match_id) REFERENCES sports_tournament_match(id),
    CONSTRAINT fk_race_result_player FOREIGN KEY (player_id) REFERENCES sports_auction_player(id),
    CONSTRAINT fk_race_result_team FOREIGN KEY (team_id) REFERENCES sports_auction_team(id)
);

CREATE INDEX idx_race_result_match ON sports_race_result(match_id);
CREATE INDEX idx_race_result_player ON sports_race_result(player_id);
CREATE INDEX idx_race_result_heat ON sports_race_result(match_id, heat_number);
