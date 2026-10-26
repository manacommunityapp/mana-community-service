package com.manacommunity.api.market.bazaar.repository;

import com.manacommunity.api.market.bazaar.entity.MarketBooking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MarketBookingRepository extends JpaRepository<MarketBooking, Long> {
    Optional<MarketBooking> findByApplicationId(Long applicationId);
    List<MarketBooking> findByStatusAndHoldExpiresAtBefore(String status, LocalDateTime time);
    long countByApplicationEventIdAndStatus(Long eventId, String status);
    @Query("select coalesce(sum(b.amount), 0) from MarketBooking b where b.application.event.id = :eventId and b.status = 'CONFIRMED'")
    java.math.BigDecimal sumConfirmedByEvent(@Param("eventId") Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from MarketBooking b where b.id = :id and b.applicant.id = :applicantId")
    Optional<MarketBooking> findByIdAndApplicantIdForUpdate(@Param("id") Long id, @Param("applicantId") Long applicantId);
}
