package com.manacommunity.api.safety.repository;

import com.manacommunity.api.safety.dto.TrustAndSafetyDtos.LocationConsentPurpose;
import com.manacommunity.api.safety.model.LocationConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationConsentRepository extends JpaRepository<LocationConsent, Long> {

    List<LocationConsent> findByUserId(Long userId);

    List<LocationConsent> findByCommunityId(Long communityId);

    Optional<LocationConsent> findByUserIdAndPurpose(Long userId, LocationConsentPurpose purpose);
}