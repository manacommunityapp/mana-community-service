package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VmsBookingRepository extends JpaRepository<VmsBooking, Long> {

    Page<VmsBooking> findByCommunityIdAndStatus(Long communityId, VmsBooking.BookingStatus status, Pageable pageable);

    Page<VmsBooking> findByCommunityId(Long communityId, Pageable pageable);

    Page<VmsBooking> findByVendorIdAndStatus(Long vendorId, VmsBooking.BookingStatus status, Pageable pageable);

    Page<VmsBooking> findByVendorId(Long vendorId, Pageable pageable);

    Page<VmsBooking> findByUserId(Long userId, Pageable pageable);

    Page<VmsBooking> findByUserIdAndStatus(Long userId, VmsBooking.BookingStatus status, Pageable pageable);

    Optional<VmsBooking> findByIdAndCommunityId(Long id, Long communityId);

    Optional<VmsBooking> findByBookingNumberAndCommunityId(String bookingNumber, Long communityId);

    @Query("SELECT b FROM VmsBooking b WHERE b.vendor.id = :vendorId AND b.scheduledDate = :date AND b.status NOT IN ('CANCELLED')")
    List<VmsBooking> findActiveBookingsByVendorAndDate(@Param("vendorId") Long vendorId, @Param("date") LocalDate date);

    long countByCommunityIdAndStatus(Long communityId, VmsBooking.BookingStatus status);

    long countByVendorIdAndStatus(Long vendorId, VmsBooking.BookingStatus status);
}
