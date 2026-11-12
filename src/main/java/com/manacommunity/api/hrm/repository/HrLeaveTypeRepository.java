package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrLeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrLeaveTypeRepository extends JpaRepository<HrLeaveType, Long> {

    List<HrLeaveType> findByOrganizationIdAndIsActiveTrue(Long organizationId);
}
