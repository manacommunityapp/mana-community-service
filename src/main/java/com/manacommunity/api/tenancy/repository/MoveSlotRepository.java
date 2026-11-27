package com.manacommunity.api.tenancy.repository;

import com.manacommunity.api.tenancy.entity.MoveSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MoveSlotRepository extends JpaRepository<MoveSlot, Long> {

    @Query("SELECT s FROM MoveSlot s WHERE s.towerName = :tower AND s.slotDate = :date AND s.slotType = :type AND s.status NOT IN ('CANCELLED', 'REJECTED')")
    List<MoveSlot> findActiveSlotsByTowerAndDate(String tower, LocalDate date, MoveSlot.SlotType type);

    @Query("SELECT s FROM MoveSlot s WHERE s.communityId = :communityId AND s.slotDate = :date AND s.status NOT IN ('CANCELLED', 'REJECTED')")
    List<MoveSlot> findAllSlotsByDate(Long communityId, LocalDate date);

    List<MoveSlot> findByTenantRecordId(Long tenantRecordId);

    @Query("SELECT s FROM MoveSlot s WHERE s.serviceElevatorId = :elevatorId AND s.slotDate = :date AND s.status NOT IN ('CANCELLED', 'REJECTED', 'COMPLETED')")
    List<MoveSlot> findElevatorBookings(String elevatorId, LocalDate date);
}
