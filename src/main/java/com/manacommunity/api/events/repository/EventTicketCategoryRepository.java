package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventTicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventTicketCategoryRepository extends JpaRepository<EventTicketCategory, Long> {

    List<EventTicketCategory> findByEventIdOrderByDisplayOrderAscIdAsc(Long eventId);

    List<EventTicketCategory> findByCommunityIdOrderByDisplayOrderAscIdAsc(Long communityId);

    @Modifying
    void deleteByEventId(Long eventId);
}
