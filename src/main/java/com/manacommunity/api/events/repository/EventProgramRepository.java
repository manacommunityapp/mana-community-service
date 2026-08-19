package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface EventProgramRepository extends JpaRepository<EventProgram, Long> {

    @Modifying
    void deleteByEventId(Long eventId);

    List<EventProgram> findByEventIdOrderBySortOrderAscStartTimeAsc(Long eventId);

    List<EventProgram> findByEventIdAndDayLabelOrderBySortOrderAscStartTimeAsc(Long eventId, String dayLabel);
}
