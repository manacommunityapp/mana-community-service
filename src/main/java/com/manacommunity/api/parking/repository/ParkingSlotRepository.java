package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.ParkingSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    List<ParkingSlot> findByCommunityIdOrderBySlotNumberAsc(Long communityId);

    List<ParkingSlot> findByCommunityIdAndStatusOrderBySlotNumberAsc(Long communityId, ParkingSlot.SlotStatus status);

    Optional<ParkingSlot> findByCommunityIdAndSlotNumber(Long communityId, String slotNumber);

    long countByCommunityId(Long communityId);

    long countByCommunityIdAndStatus(Long communityId, ParkingSlot.SlotStatus status);

    List<ParkingSlot> findByAssignedToId(Long userId);
}
