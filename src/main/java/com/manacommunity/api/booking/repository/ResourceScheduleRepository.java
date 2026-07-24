package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.ResourceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface ResourceScheduleRepository extends JpaRepository<ResourceSchedule, Long> {

    List<ResourceSchedule> findByResourceIdOrderByDayOfWeekAsc(Long resourceId);

    Optional<ResourceSchedule> findByResourceIdAndDayOfWeek(Long resourceId, DayOfWeek dayOfWeek);
}
