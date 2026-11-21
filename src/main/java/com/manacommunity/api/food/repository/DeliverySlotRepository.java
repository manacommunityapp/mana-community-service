package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.DeliverySlot;
import com.manacommunity.api.food.entity.DeliverySlot.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface DeliverySlotRepository extends JpaRepository<DeliverySlot, Long> {
    List<DeliverySlot> findByVendorIdAndCommunityIdAndSlotDateAndStatus(Long vendorId, Long communityId, LocalDate date, SlotStatus status);
    List<DeliverySlot> findByStatusAndCutoffAtBefore(SlotStatus status, Instant now);

    @Modifying
    @Query("UPDATE DeliverySlot s SET s.bookedCount = s.bookedCount + 1, s.status = CASE WHEN s.bookedCount + 1 >= s.capacity THEN 'FULL' ELSE s.status END WHERE s.id = :id AND s.bookedCount < s.capacity AND s.status = 'OPEN'")
    int incrementBookedCount(@Param("id") Long id);
}
