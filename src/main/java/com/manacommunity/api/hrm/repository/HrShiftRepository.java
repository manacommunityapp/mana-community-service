package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrShiftRepository extends JpaRepository<HrShift, Long> {

    List<HrShift> findByOrganizationIdAndIsActiveTrue(Long organizationId);
}
