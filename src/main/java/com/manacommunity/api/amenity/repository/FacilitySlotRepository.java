package com.manacommunity.api.amenity.repository;

import com.manacommunity.api.amenity.entity.FacilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacilitySlotRepository extends JpaRepository<FacilitySlot, Long> {

    List<FacilitySlot> findByFacilityIdAndSlotDate(Long facilityId, LocalDate slotDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM FacilitySlot s WHERE s.id = :id")
    Optional<FacilitySlot> findByIdWithPessimisticLock(@Param("id") Long id);

    @Query("SELECT s FROM FacilitySlot s WHERE s.facilityId = :facilityId AND s.slotDate = :date AND s.status = 'AVAILABLE'")
    List<FacilitySlot> findAvailableSlots(@Param("facilityId") Long facilityId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM FacilitySlot s WHERE s.facilityId = :facilityId AND s.bookedByFlatNo = :flatNo AND s.slotDate BETWEEN :weekStart AND :weekEnd AND s.status = 'BOOKED'")
    long countBookingsForFlatInWeek(@Param("facilityId") Long facilityId, @Param("flatNo") String flatNo,
                                     @Param("weekStart") LocalDate weekStart, @Param("weekEnd") LocalDate weekEnd);

    @Query("SELECT COUNT(s) FROM FacilitySlot s WHERE s.facilityId = :facilityId AND s.bookedByFlatNo = :flatNo AND s.isPeakHour = true AND s.slotDate >= :today AND s.status = 'BOOKED'")
    long countActivePeakBookingsForFlat(@Param("facilityId") Long facilityId, @Param("flatNo") String flatNo,
                                         @Param("today") LocalDate today);
}
