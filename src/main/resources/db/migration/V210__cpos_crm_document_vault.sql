-- Document Vault: attach files to CRM leads
CREATE TABLE cpos_crm_document (
    id           BIGSERIAL PRIMARY KEY,
    lead_id      BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    file_url     VARCHAR(1000) NOT NULL,
    file_size    BIGINT NULL,
    mime_type    VARCHAR(100) NULL,
    uploaded_by  VARCHAR(255) NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_crm_doc_lead FOREIGN KEY (lead_id) REFERENCES cpos_crm_lead (id),
    CONSTRAINT fk_crm_doc_community FOREIGN KEY (community_id) REFERENCES community (id)
);

CREATE INDEX idx_crm_doc_lead ON cpos_crm_document (lead_id);
