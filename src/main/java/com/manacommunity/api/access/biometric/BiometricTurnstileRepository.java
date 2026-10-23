package com.manacommunity.api.access.biometric;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricTurnstileRepository extends JpaRepository<BiometricTurnstile, Long> {
    Optional<BiometricTurnstile> findByTurnstileIdentifier(String identifier);
    List<BiometricTurnstile> findByCommunityId(Long communityId);
}
