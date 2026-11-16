package com.manacommunity.api.market.bazaar.repository;

import com.manacommunity.api.market.bazaar.entity.MarketApplication;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketApplicationRepository extends JpaRepository<MarketApplication, Long> {
    List<MarketApplication> findByApplicantIdOrderByRequestedAtDesc(Long applicantId);
    List<MarketApplication> findByEventIdOrderByRequestedAtDesc(Long eventId);

    @Query("select a from MarketApplication a where a.id = :id and a.event.event.community.id = :communityId")
    Optional<MarketApplication> findScoped(@Param("id") Long id, @Param("communityId") Long communityId);

    boolean existsByEventIdAndApplicantIdAndStallIdAndStatusIn(Long eventId, Long applicantId, Long stallId, List<String> statuses);
    long countByEventIdAndStatusIn(Long eventId, List<String> statuses);
}
