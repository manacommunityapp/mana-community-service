package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityCarpoolOffer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface MobilityCarpoolOfferRepository extends JpaRepository<MobilityCarpoolOffer, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM MobilityCarpoolOffer o WHERE o.id = :id")
    java.util.Optional<MobilityCarpoolOffer> findByIdForUpdate(Long id);

    @Query("SELECT o FROM MobilityCarpoolOffer o WHERE o.community.id = :communityId AND o.status = 'ACTIVE'" +
           " AND (:dest IS NULL OR LOWER(o.destination) LIKE LOWER(CONCAT('%', :dest, '%')))" +
           " ORDER BY o.departureTime ASC")
    Page<MobilityCarpoolOffer> findActive(Long communityId, String dest, Pageable pageable);

    @Query("SELECT o FROM MobilityCarpoolOffer o WHERE o.community.id = :communityId AND o.status IN ('ACTIVE', 'FULL')" +
           " AND o.airportCode = :airportCode AND o.airportDirection = :direction" +
           " AND (:area IS NULL OR LOWER(o.origin) LIKE LOWER(CONCAT('%', :area, '%')) OR LOWER(o.destination) LIKE LOWER(CONCAT('%', :area, '%')))" +
           " AND (:fromTime IS NULL OR o.departureTime >= :fromTime) AND (:toTime IS NULL OR o.departureTime < :toTime)" +
           " AND o.availableSeats >= :seats AND COALESCE(o.luggageCapacity, 0) >= :luggageCount" +
           " ORDER BY o.departureTime ASC")
    Page<MobilityCarpoolOffer> findAirportRides(@Param("communityId") Long communityId, @Param("airportCode") String airportCode, @Param("direction") String direction, @Param("area") String area, @Param("fromTime") java.time.Instant fromTime, @Param("toTime") java.time.Instant toTime, @Param("seats") int seats, @Param("luggageCount") int luggageCount, Pageable pageable);

    @Query("SELECT o FROM MobilityCarpoolOffer o WHERE o.driverUser.id = :userId AND o.airportCode IS NOT NULL ORDER BY o.departureTime DESC")
    Page<MobilityCarpoolOffer> findMyAirportRides(Long userId, Pageable pageable);

    @Query("SELECT o FROM MobilityCarpoolOffer o WHERE o.community.id = :communityId AND o.status IN ('ACTIVE', 'FULL')" +
           " AND o.journeyType = :journeyType AND o.hubCode = :hubCode" +
           " AND (:area IS NULL OR LOWER(o.origin) LIKE LOWER(CONCAT('%', :area, '%')) OR LOWER(o.destination) LIKE LOWER(CONCAT('%', :area, '%')))" +
           " AND (:fromTime IS NULL OR o.departureTime >= :fromTime) AND (:toTime IS NULL OR o.departureTime < :toTime)" +
           " AND o.availableSeats >= :seats AND COALESCE(o.luggageCapacity, 0) >= :luggageCount" +
           " ORDER BY o.departureTime ASC")
    Page<MobilityCarpoolOffer> findJourneyOffers(@Param("communityId") Long communityId,
                                                  @Param("journeyType") String journeyType,
                                                  @Param("hubCode") String hubCode,
                                                  @Param("area") String area,
                                                  @Param("fromTime") java.time.Instant fromTime,
                                                  @Param("toTime") java.time.Instant toTime,
                                                  @Param("seats") int seats,
                                                  @Param("luggageCount") int luggageCount,
                                                  Pageable pageable);
}
