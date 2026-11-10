package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrDesignation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrDesignationRepository extends JpaRepository<HrDesignation, Long> {

    List<HrDesignation> findByOrganizationId(Long organizationId);
}
