package com.manacommunity.api.iot.metering;

import com.manacommunity.api.iot.metering.MeteringEnums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterBillingCycleSummaryRepository extends JpaRepository<MeterBillingCycleSummary, Long> {
    Optional<MeterBillingCycleSummary> findByMeterIdAndBillingMonth(Long meterId, String billingMonth);
    List<MeterBillingCycleSummary> findByCommunityIdAndBillingMonth(Long communityId, String billingMonth);
    List<MeterBillingCycleSummary> findByCommunityIdAndBillingStatus(Long communityId, MeterBillingStatus status);
}
