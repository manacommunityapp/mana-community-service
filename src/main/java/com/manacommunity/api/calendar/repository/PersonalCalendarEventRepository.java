package com.manacommunity.api.calendar.repository;

import com.manacommunity.api.calendar.entity.PersonalCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface PersonalCalendarEventRepository extends JpaRepository<PersonalCalendarEvent, Long> {

    List<PersonalCalendarEvent> findByCreatorUserIdAndStartTimeBetween(Long userId, Instant from, Instant to);

    List<PersonalCalendarEvent> findByCommunityIdAndCalendarTypeAndStartTimeBetween(
            Long communityId, String calendarType, Instant from, Instant to);

    List<PersonalCalendarEvent> findByCreatorUserIdAndRruleIsNotNullAndRruleUntilAfter(Long userId, Instant date);
}
