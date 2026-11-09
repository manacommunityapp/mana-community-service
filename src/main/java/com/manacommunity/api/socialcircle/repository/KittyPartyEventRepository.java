package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KittyPartyEventRepository extends JpaRepository<KittyPartyEvent, Long> {

    List<KittyPartyEvent> findByCircleIdOrderByStartTimeAsc(Long circleId);

    List<KittyPartyEvent> findByCircleIdAndStatusOrderByStartTimeAsc(Long circleId, String status);

    @Query("SELECT e FROM KittyPartyEvent e WHERE e.circle.id = :circleId AND e.endTime >= :now ORDER BY e.startTime ASC")
    List<KittyPartyEvent> findUpcomingEvents(@Param("circleId") Long circleId, @Param("now") LocalDateTime now);

    @Query("SELECT e FROM KittyPartyEvent e WHERE e.circle.id = :circleId AND e.endTime < :now ORDER BY e.startTime DESC")
    List<KittyPartyEvent> findPastEvents(@Param("circleId") Long circleId, @Param("now") LocalDateTime now);

    List<KittyPartyEvent> findByHostUserId(Long hostUserId);
}
