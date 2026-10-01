package com.manacommunity.api.parking.ev.repository;

import com.manacommunity.api.parking.ev.entity.EvChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvChargingStationRepository extends JpaRepository<EvChargingStation, Long> {
    Optional<EvChargingStation> findByHardwareId(String hardwareId);
    List<EvChargingStation> findByCommunityIdAndActiveTrue(Long communityId);
    Optional<EvChargingStation> findByParkingSlotId(Long parkingSlotId);
}
