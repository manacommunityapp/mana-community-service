package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.ParkingSlotBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ParkingSlotBookingRepository extends JpaRepository<ParkingSlotBooking, Long> {

    List<ParkingSlotBooking> findByCommunityIdAndBookingDateAndStatusOrderByStartTimeAsc(
            Long communityId, LocalDate date, ParkingSlotBooking.BookingStatus status);

    List<ParkingSlotBooking> findByBookedByOrderByBookingDateDescStartTimeDesc(Long bookedBy);

    List<ParkingSlotBooking> findByCommunityIdAndBookingDateOrderByStartTimeAsc(
            Long communityId, LocalDate date);
}
