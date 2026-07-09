package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.AmenityBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AmenityBookingRepository extends JpaRepository<AmenityBooking, Long> {

    List<AmenityBooking> findByAmenityIdAndBookingDateAndStatusOrderByStartTimeAsc(
            Long amenityId, LocalDate bookingDate, AmenityBooking.BookingStatus status);

    @Query("SELECT b FROM AmenityBooking b WHERE b.amenity.id = :amenityId " +
           "AND b.bookingDate = :date AND b.status = 'CONFIRMED' " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<AmenityBooking> findConflicting(
            @Param("amenityId") Long amenityId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    List<AmenityBooking> findByBookedByIdAndStatusOrderByBookingDateDescStartTimeDesc(
            Long userId, AmenityBooking.BookingStatus status);

    List<AmenityBooking> findByBookedByIdOrderByBookingDateDescStartTimeDesc(Long userId);

    @Query("SELECT b FROM AmenityBooking b WHERE b.amenity.community.id = :communityId " +
           "AND b.bookingDate = :date AND b.status = 'CONFIRMED' " +
           "ORDER BY b.amenity.name, b.startTime")
    List<AmenityBooking> findTodaysByCommunity(
            @Param("communityId") Long communityId,
            @Param("date") LocalDate date);
}
