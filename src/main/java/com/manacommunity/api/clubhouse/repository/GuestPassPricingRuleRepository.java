package com.manacommunity.api.clubhouse.repository;

import com.manacommunity.api.clubhouse.entity.GuestPassPricingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuestPassPricingRuleRepository extends JpaRepository<GuestPassPricingRuleEntity, UUID> {
    List<GuestPassPricingRuleEntity> findBySocietyIdAndFacilityCodeAndDayCategory(
            Long societyId, String facilityCode, String dayCategory);
}
