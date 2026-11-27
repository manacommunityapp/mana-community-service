package com.manacommunity.api.amenity.repository;

import com.manacommunity.api.amenity.entity.FlatBookingQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FlatBookingQuotaRepository extends JpaRepository<FlatBookingQuota, Long> {

    Optional<FlatBookingQuota> findByCommunityIdAndFlatNoAndFacilityIdAndWeekStart(
            Long communityId, String flatNo, Long facilityId, LocalDate weekStart);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM FlatBookingQuota q WHERE q.communityId = :communityId AND q.flatNo = :flatNo AND q.facilityId = :facilityId AND q.weekStart = :weekStart")
    Optional<FlatBookingQuota> findForUpdate(@Param("communityId") Long communityId,
                                              @Param("flatNo") String flatNo,
                                              @Param("facilityId") Long facilityId,
                                              @Param("weekStart") LocalDate weekStart);
}
