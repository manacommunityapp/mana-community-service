package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.BookingEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingEquipmentRepository extends JpaRepository<BookingEquipment, Long> {

    List<BookingEquipment> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);
}
