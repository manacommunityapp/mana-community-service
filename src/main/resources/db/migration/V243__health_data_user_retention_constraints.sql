ALTER TABLE manacommunity.health_data_consent
    ADD CONSTRAINT fk_health_data_consent_user
        FOREIGN KEY (user_id) REFERENCES manacommunity.app_user(id) ON DELETE CASCADE;

ALTER TABLE manacommunity.health_observation
    ADD CONSTRAINT fk_health_observation_user
        FOREIGN KEY (user_id) REFERENCES manacommunity.app_user(id) ON DELETE CASCADE;
