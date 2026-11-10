package com.manacommunity.api.market.bazaar.repository;

import com.manacommunity.api.market.bazaar.entity.MarketStall;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MarketStallRepository extends JpaRepository<MarketStall, Long> {
    List<MarketStall> findByEventIdOrderByStallNumberAsc(Long eventId);
    long countByEventIdAndStatus(Long eventId, String status);
    long countByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from MarketStall s where s.id = :stallId and s.event.id = :eventId")
    Optional<MarketStall> findByIdAndEventIdForUpdate(@Param("stallId") Long stallId, @Param("eventId") Long eventId);
}

