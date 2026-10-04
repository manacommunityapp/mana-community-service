package com.manacommunity.api.parking.ev.repository;

import com.manacommunity.api.parking.ev.entity.EvCharger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvChargerRepository extends JpaRepository<EvCharger, Long> {

    List<EvCharger> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    Optional<EvCharger> findByDeviceId(String deviceId);

    Optional<EvCharger> findByParkingSlotId(Long parkingSlotId);

    List<EvCharger> findByCommunityIdAndStatus(Long communityId, EvCharger.ChargerStatus status);
}