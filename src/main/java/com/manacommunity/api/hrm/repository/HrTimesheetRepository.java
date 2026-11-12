package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrTimesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HrTimesheetRepository extends JpaRepository<HrTimesheet, Long> {
    Optional<HrTimesheet> findByEmployeeIdAndWeekStartDate(Long employeeId, LocalDate weekStartDate);
    List<HrTimesheet> findByEmployeeIdOrderByWeekStartDateDesc(Long employeeId);
    Page<HrTimesheet> findByOrganizationIdAndStatus(Long orgId, String status, Pageable pageable);
}
