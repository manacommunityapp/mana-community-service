-- Rename cricheroes_profile table to sports_cricheroes_profile
ALTER TABLE cricheroes_profile RENAME TO sports_cricheroes_profile;

-- Rename indexes
ALTER INDEX IF EXISTS idx_cricheroes_profile_player RENAME TO idx_sports_cricheroes_profile_player;
ALTER INDEX IF EXISTS idx_cricheroes_profile_config RENAME TO idx_sports_cricheroes_profile_config;
ALTER INDEX IF EXISTS idx_cricheroes_profile_ch_id RENAME TO idx_sports_cricheroes_profile_ch_id;

-- Rename constraints
ALTER TABLE sports_cricheroes_profile RENAME CONSTRAINT uq_cricheroes_player TO uq_sports_cricheroes_player;
ALTER TABLE sports_cricheroes_profile RENAME CONSTRAINT uq_cricheroes_id_community TO uq_sports_cricheroes_id_community;
