CREATE TABLE search_index (
    id              BIGSERIAL    PRIMARY KEY,
    community_id    BIGINT       NOT NULL,
    module          VARCHAR(60)  NOT NULL,
    entity_id       VARCHAR(100) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    subtitle        VARCHAR(500),
    description     TEXT,
    tags            TEXT,
    image_url       VARCHAR(1000),
    deep_link_type  VARCHAR(60)  NOT NULL,
    deep_link_id    VARCHAR(100) NOT NULL,
    visibility      VARCHAR(30)  NOT NULL DEFAULT 'COMMUNITY',
    owner_user_id   BIGINT,
    metadata        TEXT,
    search_vector   TSVECTOR,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    indexed_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(community_id, module, entity_id)
);

CREATE OR REPLACE FUNCTION search_index_update_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.subtitle, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(NEW.tags, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(NEW.description, '')), 'C');
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_search_index_vector
BEFORE INSERT OR UPDATE OF title, subtitle, description, tags ON search_index
FOR EACH ROW EXECUTE FUNCTION search_index_update_vector();

CREATE INDEX idx_search_fts       ON search_index USING GIN(search_vector);
CREATE INDEX idx_search_community ON search_index(community_id, is_active);
CREATE INDEX idx_search_module    ON search_index(community_id, module, is_active);
CREATE INDEX idx_search_owner     ON search_index(owner_user_id);
