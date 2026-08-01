package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.ResourceHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ResourceHolidayRepository extends JpaRepository<ResourceHoliday, Long> {

    List<ResourceHoliday> findByResourceIdAndHolidayDateBetweenOrderByHolidayDateAsc(Long resourceId, LocalDate from, LocalDate to);

    List<ResourceHoliday> findByCommunityIdAndHolidayDateBetween(Long communityId, LocalDate from, LocalDate to);

    boolean existsByResourceIdAndHolidayDate(Long resourceId, LocalDate date);
}
