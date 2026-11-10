package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrDesignation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HrDesignationRepository extends JpaRepository<HrDesignation, Long> {

    List<HrDesignation> findByOrganizationId(Long organizationId);

    Optional<HrDesignation> findByIdAndOrganizationId(Long id, Long organizationId);
}
