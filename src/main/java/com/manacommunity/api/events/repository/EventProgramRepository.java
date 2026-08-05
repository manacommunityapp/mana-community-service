package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventProgramRepository extends JpaRepository<EventProgram, Long> {

    List<EventProgram> findByEventIdOrderBySortOrderAscStartTimeAsc(Long eventId);

    List<EventProgram> findByEventIdAndDayLabelOrderBySortOrderAscStartTimeAsc(Long eventId, String dayLabel);
}
