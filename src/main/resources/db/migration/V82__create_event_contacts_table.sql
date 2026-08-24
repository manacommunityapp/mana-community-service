-- V82: Create event_contacts table to normalize committee and event organizer contacts
-- from contacts_json column in event_community into a dedicated table.

DO $$
BEGIN
    CREATE TABLE IF NOT EXISTS manacommunity.event_contacts (
        id                BIGSERIAL PRIMARY KEY,
        event_id          BIGINT NOT NULL REFERENCES manacommunity.event_community(id) ON DELETE CASCADE,
        community_id      BIGINT,
        contact_code      VARCHAR(100),
        name              VARCHAR(150) NOT NULL,
        phone             VARCHAR(50),
        email             VARCHAR(100),
        role              VARCHAR(100),
        notes             VARCHAR(1000),
        display_order     INTEGER NOT NULL DEFAULT 1,
        is_primary        BOOLEAN NOT NULL DEFAULT FALSE,
        created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by        BIGINT NOT NULL DEFAULT 0,
        updated_by        BIGINT NOT NULL DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_event_contacts_event_id ON manacommunity.event_contacts (event_id);
    CREATE INDEX IF NOT EXISTS idx_event_contacts_community_id ON manacommunity.event_contacts (community_id);
END $$;
