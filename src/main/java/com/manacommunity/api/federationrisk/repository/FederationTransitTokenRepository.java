package com.manacommunity.api.federationrisk.repository;

import com.manacommunity.api.federationrisk.entity.FederationTransitToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FederationTransitTokenRepository extends JpaRepository<FederationTransitToken, Long> {
    Optional<FederationTransitToken> findByTransitId(String transitId);
    List<FederationTransitToken> findByTargetSocietyIdAndConsumedAtIsNullAndValidUntilAfter(Long targetSocietyId, Instant now);
}
