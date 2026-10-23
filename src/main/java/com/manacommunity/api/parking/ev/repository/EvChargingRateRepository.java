package com.manacommunity.api.parking.ev.repository;

import com.manacommunity.api.parking.ev.entity.EvChargingRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvChargingRateRepository extends JpaRepository<EvChargingRate, Long> {

    Optional<EvChargingRate> findFirstByCommunityIdAndActiveTrue(Long communityId);
}