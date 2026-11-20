package com.manacommunity.api.communityops.pass;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DigitalPassRepository extends JpaRepository<DigitalPass, Long> {
    Optional<DigitalPass> findByToken(String token);
    List<DigitalPass> findByCreatedByAndStatusAndValidUntilAfter(Long createdBy, PassStatus status, Instant now);
    List<DigitalPass> findByCommunityIdAndFlatNumberAndStatusAndValidUntilAfter(Long communityId, String flatNumber, PassStatus status, Instant now);
    long countByCommunityIdAndValidFromBetween(Long communityId, Instant from, Instant to);
}
