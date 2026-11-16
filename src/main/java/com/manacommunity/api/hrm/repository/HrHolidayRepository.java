package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HrHolidayRepository extends JpaRepository<HrHoliday, Long> {

    List<HrHoliday> findByOrganizationIdAndHolidayDateBetween(Long organizationId, LocalDate from, LocalDate to);
}
