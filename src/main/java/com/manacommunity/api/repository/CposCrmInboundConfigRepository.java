package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposCrmInboundConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CposCrmInboundConfigRepository extends JpaRepository<CposCrmInboundConfig, Long> {
    Optional<CposCrmInboundConfig> findByCommunityId(Long communityId);
    Optional<CposCrmInboundConfig> findByApiKey(String apiKey);
}
