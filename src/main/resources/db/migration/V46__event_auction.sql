-- Migration V46: Event fundraising auction (items + bids)

CREATE TABLE IF NOT EXISTS manacommunity.event_auction_item (
    id             BIGSERIAL    PRIMARY KEY,
    community_id   BIGINT       NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    event_id       BIGINT       REFERENCES manacommunity.community_event(id) ON DELETE CASCADE,
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    category       VARCHAR(100),
    base_price     DECIMAL(12,2) NOT NULL DEFAULT 0,
    current_bid    DECIMAL(12,2) NOT NULL DEFAULT 0,
    min_increment  DECIMAL(12,2) NOT NULL DEFAULT 500,
    image_emoji    VARCHAR(10),
    image_url      VARCHAR(500),
    status         VARCHAR(20)  NOT NULL DEFAULT 'UPCOMING',
    sort_order     INT          NOT NULL DEFAULT 0,
    bid_count      INT          NOT NULL DEFAULT 0,
    leader_name    VARCHAR(200),
    closed_at      TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS manacommunity.event_auction_bid (
    id              BIGSERIAL    PRIMARY KEY,
    item_id         BIGINT       NOT NULL REFERENCES manacommunity.event_auction_item(id) ON DELETE CASCADE,
    community_id    BIGINT       NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    event_id        BIGINT       REFERENCES manacommunity.community_event(id),
    bidder_user_id  BIGINT,
    bidder_name     VARCHAR(200) NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    bid_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_event_auction_item_community
    ON manacommunity.event_auction_item(community_id);

CREATE INDEX IF NOT EXISTS idx_event_auction_item_event_status
    ON manacommunity.event_auction_item(event_id, status);

CREATE INDEX IF NOT EXISTS idx_event_auction_bid_item
    ON manacommunity.event_auction_bid(item_id);

CREATE INDEX IF NOT EXISTS idx_event_auction_bid_community_time
    ON manacommunity.event_auction_bid(community_id, bid_at DESC);
