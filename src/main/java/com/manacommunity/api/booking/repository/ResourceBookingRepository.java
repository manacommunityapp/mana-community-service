package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.ResourceBooking;
import com.manacommunity.api.booking.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ResourceBookingRepository extends JpaRepository<ResourceBooking, Long> {

    List<ResourceBooking> findByResourceIdAndBookingDateAndStatusInOrderByStartTimeAsc(
            Long resourceId, LocalDate date, List<BookingStatus> statuses);

    List<ResourceBooking> findByBookedByIdOrderByBookingDateDescStartTimeDesc(Long userId);

    List<ResourceBooking> findByBookedByIdAndStatusOrderByBookingDateDesc(Long userId, BookingStatus status);

    @Query("SELECT b FROM ResourceBooking b WHERE b.resource.id = :resourceId " +
           "AND b.bookingDate = :date " +
           "AND b.status IN ('CONFIRMED', 'CHECKED_IN') " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<ResourceBooking> findConflicting(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("SELECT b FROM ResourceBooking b WHERE b.resource.community.id = :communityId " +
           "AND b.bookingDate = :date " +
           "AND b.status IN ('CONFIRMED', 'CHECKED_IN') " +
           "ORDER BY b.resource.name, b.startTime")
    List<ResourceBooking> findTodaysByCommunity(
            @Param("communityId") Long communityId,
            @Param("date") LocalDate date);

    long countByBookedByIdAndResourceIdAndStatusIn(Long userId, Long resourceId, List<BookingStatus> statuses);

    long countByBookedByIdAndStatusIn(Long userId, List<BookingStatus> statuses);

    Optional<ResourceBooking> findByBookingNumber(String bookingNumber);

    @Query("SELECT COUNT(b) FROM ResourceBooking b WHERE b.resource.id = :resourceId " +
           "AND b.bookingDate BETWEEN :from AND :to AND b.status = :status")
    long countByResourceIdAndBookingDateBetweenAndStatus(
            @Param("resourceId") Long resourceId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") BookingStatus status);

    List<ResourceBooking> findByCommunityIdAndBookingDateBetweenOrderByBookingDateAsc(
            Long communityId, LocalDate from, LocalDate to);

    Page<ResourceBooking> findByCommunityIdAndStatusIn(
            Long communityId, List<BookingStatus> statuses, Pageable pageable);
}
