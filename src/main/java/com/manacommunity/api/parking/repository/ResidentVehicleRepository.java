package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.ResidentVehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResidentVehicleRepository extends JpaRepository<ResidentVehicle, Long> {

    List<ResidentVehicle> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<ResidentVehicle> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<ResidentVehicle> findByCommunityIdAndNumberPlate(Long communityId, String numberPlate);

    long countByCommunityId(Long communityId);

    long countByOwnerId(Long ownerId);

    List<ResidentVehicle> findByParkingSlotId(Long slotId);
}
