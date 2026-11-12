package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HrLeaveBalanceRepository extends JpaRepository<HrLeaveBalance, Long> {

    List<HrLeaveBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);

    Optional<HrLeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, Integer year);
}
