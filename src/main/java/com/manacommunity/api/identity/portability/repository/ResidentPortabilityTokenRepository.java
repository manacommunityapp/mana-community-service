package com.manacommunity.api.identity.portability.repository;

import com.manacommunity.api.identity.portability.entity.ResidentPortabilityToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ResidentPortabilityTokenRepository extends JpaRepository<ResidentPortabilityToken, Long> {

    Optional<ResidentPortabilityToken> findTopByResidentIdAndSourceCommunityIdAndExpiresAtAfterOrderByCreatedAtDesc(
            Long residentId, Long sourceCommunityId, Instant now);

    Optional<ResidentPortabilityToken> findByTokenHashAndExpiresAtAfterAndClaimedAtIsNull(
            String tokenHash, Instant now);
}
