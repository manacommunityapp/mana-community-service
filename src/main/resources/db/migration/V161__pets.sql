-- V160: Pet registration module

CREATE TABLE IF NOT EXISTS manacommunity.pet (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT NOT NULL REFERENCES manacommunity.community(id),
    owner_id            BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    name                VARCHAR(100) NOT NULL,
    species             VARCHAR(50)  NOT NULL DEFAULT 'DOG',  -- DOG, CAT, BIRD, FISH, RABBIT, OTHER
    breed               VARCHAR(100),
    color               VARCHAR(80),
    age_months          INT,
    weight_kg           NUMERIC(5,2),
    microchip_id        VARCHAR(50),
    is_vaccinated       BOOLEAN      NOT NULL DEFAULT FALSE,
    vaccine_expiry_date DATE,
    registration_date   DATE         NOT NULL DEFAULT CURRENT_DATE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, DECEASED
    image_url           VARCHAR(500),
    notes               TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT
);

CREATE INDEX IF NOT EXISTS idx_pet_community   ON manacommunity.pet(community_id);
CREATE INDEX IF NOT EXISTS idx_pet_owner       ON manacommunity.pet(owner_id);
CREATE INDEX IF NOT EXISTS idx_pet_status      ON manacommunity.pet(status);
