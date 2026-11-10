package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrBranchRepository extends JpaRepository<HrBranch, Long> {

    List<HrBranch> findByOrganizationId(Long organizationId);
}
