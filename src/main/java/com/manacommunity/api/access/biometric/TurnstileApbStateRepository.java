package com.manacommunity.api.access.biometric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TurnstileApbStateRepository extends JpaRepository<TurnstileApbState, Long> {
    Optional<TurnstileApbState> findByCommunityIdAndZoneIdAndCredentialId(Long communityId, String zoneId, String credentialId);
}
