package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrEmployeeShift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HrEmployeeShiftRepository extends JpaRepository<HrEmployeeShift, Long> {

    List<HrEmployeeShift> findByEmployeeIdAndShiftDateBetween(Long employeeId, LocalDate from, LocalDate to);

    Page<HrEmployeeShift> findByOrganizationIdAndShiftDate(Long organizationId, LocalDate date, Pageable pageable);
}
