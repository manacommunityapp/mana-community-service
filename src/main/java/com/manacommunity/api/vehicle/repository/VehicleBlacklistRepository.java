package com.manacommunity.api.vehicle.repository;

import com.manacommunity.api.vehicle.entity.VehicleBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleBlacklistRepository extends JpaRepository<VehicleBlacklist, Long> {
    List<VehicleBlacklist> findByCommunityIdAndIsActiveTrue(Long communityId);
    Optional<VehicleBlacklist> findByCommunityIdAndPlateNumberAndIsActiveTrue(Long communityId, String plateNumber);
    boolean existsByCommunityIdAndPlateNumberAndIsActiveTrue(Long communityId, String plateNumber);
}
