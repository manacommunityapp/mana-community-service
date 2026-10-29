package com.manacommunity.api.helpdesk.repository;

import com.manacommunity.api.helpdesk.entity.VendorTrackingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VendorTrackingSessionRepository extends JpaRepository<VendorTrackingSession, Long> {

    @Query("SELECT t FROM VendorTrackingSession t WHERE t.bookingId = :bookingId ORDER BY t.recordedAt DESC LIMIT 1")
    Optional<VendorTrackingSession> findLatestByBookingId(@Param("bookingId") Long bookingId);
}
