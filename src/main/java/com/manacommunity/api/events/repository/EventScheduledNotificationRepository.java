package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventScheduledNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventScheduledNotificationRepository extends JpaRepository<EventScheduledNotification, Long> {

    List<EventScheduledNotification> findByEventIdOrderByCreatedAtDesc(Long eventId);

    Page<EventScheduledNotification> findByEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    Page<EventScheduledNotification> findByEventIdAndStatusOrderByCreatedAtDesc(Long eventId, String status, Pageable pageable);

    long countByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, String status);

    @Query("SELECT n FROM EventScheduledNotification n WHERE n.event.id = :eventId")
    List<EventScheduledNotification> findAllByEventId(@Param("eventId") Long eventId);
}
