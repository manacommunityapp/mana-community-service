package com.manacommunity.api.calendar.repository;

import com.manacommunity.api.calendar.model.CalendarDomain;
import com.manacommunity.api.calendar.model.CalendarEventItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEventItem, Long> {

    @Query("SELECT e FROM CalendarEventItem e WHERE e.startTime >= :from AND e.startTime <= :to ORDER BY e.startTime ASC")
    List<CalendarEventItem> findBetweenDates(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT e FROM CalendarEventItem e WHERE e.domain = :domain AND e.startTime >= :from AND e.startTime <= :to ORDER BY e.startTime ASC")
    List<CalendarEventItem> findByDomainBetweenDates(
            @Param("domain") CalendarDomain domain,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
