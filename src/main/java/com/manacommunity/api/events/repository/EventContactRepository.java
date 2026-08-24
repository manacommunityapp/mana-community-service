package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventContactRepository extends JpaRepository<EventContact, Long> {

    List<EventContact> findByEventIdOrderByDisplayOrderAsc(Long eventId);

    List<EventContact> findByCommunityId(Long communityId);

    @Modifying
    @Query("DELETE FROM EventContact ec WHERE ec.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}
