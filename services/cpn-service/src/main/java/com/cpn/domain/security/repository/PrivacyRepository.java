package com.cpn.domain.security.repository;

import com.cpn.domain.security.model.PrivacySetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrivacyRepository extends JpaRepository<PrivacySetting, UUID> {
    Optional<PrivacySetting> findByUserId(UUID userId);
}
