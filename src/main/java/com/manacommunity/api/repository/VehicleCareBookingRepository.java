package com.manacommunity.api.repository;

import com.manacommunity.api.model.VehicleCareBooking;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleCareBookingRepository extends JpaRepository<VehicleCareBooking, Long> {

    Page<VehicleCareBooking> findByResidentOrderByCreatedAtDesc(AppUser resident, Pageable pageable);

    Optional<VehicleCareBooking> findByBookingNumber(String bookingNumber);

    Optional<VehicleCareBooking> findByIdAndResident(Long id, AppUser resident);

    @Query("SELECT b FROM VehicleCareBooking b WHERE b.technician = :tech AND b.status IN :statuses ORDER BY b.createdAt DESC")
    List<VehicleCareBooking> findJobsForTechnician(@Param("tech") AppUser technician, @Param("statuses") List<String> statuses);

    @Query("SELECT b FROM VehicleCareBooking b WHERE b.communityId = :cid AND b.status IN :statuses ORDER BY b.slot.slotDate ASC, b.slot.startTime ASC")
    Page<VehicleCareBooking> findByCommunityAndStatuses(@Param("cid") Long communityId, @Param("statuses") List<String> statuses, Pageable pageable);
}
