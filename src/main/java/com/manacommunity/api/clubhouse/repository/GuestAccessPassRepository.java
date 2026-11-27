package com.manacommunity.api.clubhouse.repository;

import com.manacommunity.api.clubhouse.entity.GuestAccessPassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuestAccessPassRepository extends JpaRepository<GuestAccessPassEntity, UUID> {
    Optional<GuestAccessPassEntity> findByJti(String jti);
    List<GuestAccessPassEntity> findBySocietyIdAndFlatIdAndBillingMonthOrderByCreatedAtDesc(
            Long societyId, Long flatId, String billingMonth);
    List<GuestAccessPassEntity> findBySocietyIdAndFacilityCodeAndStatusOrderByConsumedAtDesc(
            Long societyId, String facilityCode, String status);
}
