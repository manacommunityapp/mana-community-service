-- Lead Tags: community-scoped tag vocabulary + many-to-many join
CREATE TABLE cpos_crm_tag (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_id   BIGINT       NOT NULL,
    name           VARCHAR(50)  NOT NULL,
    color          VARCHAR(20)  NOT NULL DEFAULT '#4F6AF5',
    created_at     DATETIME     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_crm_tag_community FOREIGN KEY (community_id) REFERENCES community (id),
    UNIQUE KEY uq_crm_tag_community_name (community_id, name)
);

CREATE TABLE cpos_crm_lead_tag (
    lead_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (lead_id, tag_id),
    CONSTRAINT fk_lead_tag_lead FOREIGN KEY (lead_id) REFERENCES cpos_crm_lead (id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_tag_tag  FOREIGN KEY (tag_id)  REFERENCES cpos_crm_tag  (id) ON DELETE CASCADE
);
