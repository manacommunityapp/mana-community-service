package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventLunchDinner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LunchDinnerRepository extends JpaRepository<EventLunchDinner, Long> {

    List<EventLunchDinner> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<EventLunchDinner> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);

    /** Find the meal config for capacity checks — matches event + date + mealType case-insensitively. */
    @Query("""
           SELECT m FROM EventLunchDinner m
           WHERE m.mainEventId = :eventId
             AND m.date = :date
             AND UPPER(m.mealType) = UPPER(:mealType)
           """)
    Optional<EventLunchDinner> findByEventAndDateAndType(@Param("eventId") Long eventId,
                                                         @Param("date") LocalDate date,
                                                         @Param("mealType") String mealType);
}
