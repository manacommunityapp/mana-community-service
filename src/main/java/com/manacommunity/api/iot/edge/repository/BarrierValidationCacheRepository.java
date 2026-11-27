package com.manacommunity.api.iot.edge.repository;

import com.manacommunity.api.iot.edge.entity.BarrierValidationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BarrierValidationCacheRepository extends JpaRepository<BarrierValidationCache, Long> {

    Optional<BarrierValidationCache> findByCommunityIdAndCredentialTypeAndCredentialValue(
            Long communityId, String credentialType, String credentialValue);

    List<BarrierValidationCache> findByCommunityId(Long communityId);

    void deleteByValidUntilBefore(Instant before);

    long countByCommunityId(Long communityId);
}
