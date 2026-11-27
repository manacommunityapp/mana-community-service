package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.entity.MorningDeliveryInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MorningDeliveryInstanceRepository extends JpaRepository<MorningDeliveryInstance, Long> {
    Optional<MorningDeliveryInstance> findByPlanIdAndDeliveryDate(Long planId, LocalDate deliveryDate);
    List<MorningDeliveryInstance> findByVendorIdAndDeliveryDateOrderByCommunityIdAscTowerBlockAscFloorNumberDescFlatNumberAsc(
            Long vendorId, LocalDate deliveryDate);
    List<MorningDeliveryInstance> findByDeliveryDateAndDeliveryStatus(LocalDate deliveryDate, String deliveryStatus);
}
