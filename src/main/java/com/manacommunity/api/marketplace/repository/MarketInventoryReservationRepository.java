package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketInventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MarketInventoryReservationRepository extends JpaRepository<MarketInventoryReservation, Long> {

    List<MarketInventoryReservation> findByListingIdAndStatus(Long listingId, MarketInventoryReservation.ReservationStatus status);

    List<MarketInventoryReservation> findByUserIdAndStatus(Long userId, MarketInventoryReservation.ReservationStatus status);

    List<MarketInventoryReservation> findByOrderNumber(String orderNumber);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM MarketInventoryReservation r WHERE r.listingId = :listingId AND r.status = 'RESERVED' AND r.reservedUntil > :now")
    int countActiveReservedQuantity(@Param("listingId") Long listingId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MarketInventoryReservation r SET r.status = 'EXPIRED' WHERE r.status = 'RESERVED' AND r.reservedUntil <= :now")
    int expireOutdatedReservations(@Param("now") LocalDateTime now);
}
