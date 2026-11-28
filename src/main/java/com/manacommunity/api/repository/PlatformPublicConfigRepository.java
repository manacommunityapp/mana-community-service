package com.manacommunity.api.repository;

import com.manacommunity.api.model.PlatformPublicConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformPublicConfigRepository extends JpaRepository<PlatformPublicConfig, Long> {
    Optional<PlatformPublicConfig> findByConfigKey(String configKey);
}
