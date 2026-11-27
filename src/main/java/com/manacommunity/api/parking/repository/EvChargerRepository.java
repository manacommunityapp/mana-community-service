package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.EvCharger;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvChargerRepository extends JpaRepository<EvCharger, Long> {

    List<EvCharger> findByCommunityId(Long communityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM EvCharger c WHERE c.id = :id")
    Optional<EvCharger> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT c FROM EvCharger c WHERE c.communityId = :communityId AND c.status = 'AVAILABLE' ORDER BY c.id ASC")
    List<EvCharger> findAvailableChargers(@Param("communityId") Long communityId);

    @Query("SELECT COUNT(c) FROM EvCharger c WHERE c.communityId = :communityId AND c.status = 'AVAILABLE'")
    long countAvailable(@Param("communityId") Long communityId);
}
