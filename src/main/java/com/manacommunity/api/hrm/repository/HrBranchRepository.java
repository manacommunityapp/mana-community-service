package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HrBranchRepository extends JpaRepository<HrBranch, Long> {

    List<HrBranch> findByOrganizationId(Long organizationId);

    Optional<HrBranch> findByIdAndOrganizationId(Long id, Long organizationId);
}
