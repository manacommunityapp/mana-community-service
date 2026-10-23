-- Rename cricheroes_profile table to sports_cricheroes_profile (only if the old name exists)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'cricheroes_profile') THEN
    ALTER TABLE cricheroes_profile RENAME TO sports_cricheroes_profile;
  END IF;
END $$;

-- Rename indexes (IF EXISTS handles missing indexes gracefully)
ALTER INDEX IF EXISTS idx_cricheroes_profile_player RENAME TO idx_sports_cricheroes_profile_player;
ALTER INDEX IF EXISTS idx_cricheroes_profile_config RENAME TO idx_sports_cricheroes_profile_config;
ALTER INDEX IF EXISTS idx_cricheroes_profile_ch_id RENAME TO idx_sports_cricheroes_profile_ch_id;

-- Rename constraints (only if the table was renamed above and old constraints exist)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'uq_cricheroes_player') THEN
    ALTER TABLE sports_cricheroes_profile RENAME CONSTRAINT uq_cricheroes_player TO uq_sports_cricheroes_player;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'uq_cricheroes_id_community') THEN
    ALTER TABLE sports_cricheroes_profile RENAME CONSTRAINT uq_cricheroes_id_community TO uq_sports_cricheroes_id_community;
  END IF;
END $$;
