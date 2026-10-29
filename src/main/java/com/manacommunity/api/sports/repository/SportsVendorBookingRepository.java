package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsVendorBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SportsVendorBookingRepository extends JpaRepository<SportsVendorBooking, Long> {

    Optional<SportsVendorBooking> findByBookingNumber(String bookingNumber);

    Optional<SportsVendorBooking> findByQrCodeToken(String qrCodeToken);

    List<SportsVendorBooking> findByResidentIdOrderByBookingDateDesc(Long residentId);

    List<SportsVendorBooking> findByVenueIdAndBookingDateOrderByStartTimeAsc(Long venueId, LocalDate bookingDate);

    List<SportsVendorBooking> findByVenueIdOrderByBookingDateDesc(Long venueId);
}
