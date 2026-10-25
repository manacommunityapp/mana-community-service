package com.manacommunity.api.market.bazaar.repository;

import com.manacommunity.api.market.bazaar.entity.CommunityMarketEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketEventRepository extends JpaRepository<CommunityMarketEvent, Long> {
    @Query("""
        select m from CommunityMarketEvent m
        where m.event.community.id = :communityId and m.status in :statuses
        order by m.event.startDate asc, m.event.startTime asc
        """)
    List<CommunityMarketEvent> findPublished(@Param("communityId") Long communityId, @Param("statuses") List<String> statuses);

    @Query("""
        select m from CommunityMarketEvent m
        where m.event.community.id = :communityId
        order by m.event.startDate desc, m.event.startTime desc
        """)
    List<CommunityMarketEvent> findOrganizerEvents(@Param("communityId") Long communityId);

    @Query("select m from CommunityMarketEvent m where m.id = :id and m.event.community.id = :communityId")
    Optional<CommunityMarketEvent> findScoped(@Param("id") Long id, @Param("communityId") Long communityId);

    boolean existsByEventId(Long eventId);
}
