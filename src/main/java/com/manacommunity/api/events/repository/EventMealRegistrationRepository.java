package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventMealRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventMealRegistrationRepository extends JpaRepository<EventMealRegistration, Long> {

    @Modifying
    void deleteByEventId(Long eventId);

    List<EventMealRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    void deleteByEventIdAndUserId(Long eventId, Long userId);

    long countByUserId(Long userId);

    @Query("SELECT m FROM EventMealRegistration m WHERE m.event.id = :eventId ORDER BY m.mealDate, m.mealType")
    List<EventMealRegistration> findByEventIdOrdered(@Param("eventId") Long eventId);

    List<EventMealRegistration> findByEventId(Long eventId);

    List<EventMealRegistration> findByCommunityId(Long communityId);

    @Query("SELECT COALESCE(SUM(m.headCount), 0) FROM EventMealRegistration m WHERE m.event.community.id = :communityId")
    long sumHeadCountByCommunity(@Param("communityId") Long communityId);
}
