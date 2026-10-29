package com.manacommunity.api.repository;

import com.manacommunity.api.model.VehicleServiceSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VehicleServiceSlotRepository extends JpaRepository<VehicleServiceSlot, Long> {

    List<VehicleServiceSlot> findByCommunityIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
            Long communityId, LocalDate from, LocalDate to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM VehicleServiceSlot s WHERE s.id = :id")
    Optional<VehicleServiceSlot> findByIdForUpdate(@Param("id") Long id);
}
