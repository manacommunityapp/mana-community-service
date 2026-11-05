-- V233: Expand chk_match_status check constraint to include DRAFT, PUBLISHED, and AUTO_ADVANCED
ALTER TABLE sports_tournament_match DROP CONSTRAINT IF EXISTS chk_match_status;

ALTER TABLE sports_tournament_match ADD CONSTRAINT chk_match_status CHECK (status IN (
    'DRAFT', 'PUBLISHED', 'SCHEDULED', 'LIVE', 'COMPLETED', 'POSTPONED', 'CANCELLED', 'BYE', 'AUTO_ADVANCED'
));
