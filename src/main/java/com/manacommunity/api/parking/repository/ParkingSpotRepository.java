package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long>, JpaSpecificationExecutor<ParkingSpot> {

    List<ParkingSpot> findByCommunityId(Long communityId);

    List<ParkingSpot> findByCommunityIdAndStatus(Long communityId, String status);

    List<ParkingSpot> findByCommunityIdAndAssignedUserId(Long communityId, Long userId);

    List<ParkingSpot> findByAssignedUserId(Long userId);

    long countByCommunityId(Long communityId);
}
