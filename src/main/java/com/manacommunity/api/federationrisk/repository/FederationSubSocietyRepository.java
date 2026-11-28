package com.manacommunity.api.federationrisk.repository;

import com.manacommunity.api.federationrisk.entity.FederationSubSociety;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FederationSubSocietyRepository extends JpaRepository<FederationSubSociety, Long> {
    Optional<FederationSubSociety> findByFederationIdAndSocietyId(Long federationId, Long societyId);
}
