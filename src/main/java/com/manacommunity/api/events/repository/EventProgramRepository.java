package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventProgramRepository extends JpaRepository<EventProgram, Long> {

    List<EventProgram> findByEventIdOrderBySortOrderAsc(Long eventId);

    List<EventProgram> findByEventIdAndDayLabelOrderBySortOrderAsc(Long eventId, String dayLabel);

    List<EventProgram> findByEventIdAndRequiresRegistrationTrueOrderBySortOrderAsc(Long eventId);

    List<EventProgram> findByEventCommunityIdOrderByCreatedAtDesc(Long communityId);
}
