package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.WellnessProvider;
import com.manacommunity.api.wellness.enums.WellnessEnums.WellnessProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WellnessProviderRepository extends JpaRepository<WellnessProvider, Long> {
    List<WellnessProvider> findByProviderTypeAndActiveTrue(WellnessProviderType providerType);
    List<WellnessProvider> findByActiveTrue();
    List<WellnessProvider> findByCommunityIdAndActiveTrue(Long communityId);
}
