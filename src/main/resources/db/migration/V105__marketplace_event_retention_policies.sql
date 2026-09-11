-- V105: Add MARKETPLACE_ORDERS and EVENT_REGISTRATIONS default retention policies
-- These were not present in V104. Safe to re-run — uses ON CONFLICT DO NOTHING.

INSERT INTO manacommunity.data_retention_policy
    (community_id, data_category, retention_period_days, action_on_expiry, is_active, description)
SELECT NULL, 'MARKETPLACE_ORDERS', 365, 'ANONYMIZE', TRUE,
       'Marketplace order delivery address anonymized after 1 year (financial record retained)'
WHERE NOT EXISTS (
    SELECT 1 FROM manacommunity.data_retention_policy
    WHERE community_id IS NULL AND data_category = 'MARKETPLACE_ORDERS'
);

INSERT INTO manacommunity.data_retention_policy
    (community_id, data_category, retention_period_days, action_on_expiry, is_active, description)
SELECT NULL, 'EVENT_REGISTRATIONS', 180, 'ANONYMIZE', TRUE,
       'Event registration contact details retention period (180 days)'
WHERE NOT EXISTS (
    SELECT 1 FROM manacommunity.data_retention_policy
    WHERE community_id IS NULL AND data_category = 'EVENT_REGISTRATIONS'
);
