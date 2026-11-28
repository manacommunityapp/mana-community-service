package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrContractorAgency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HrContractorAgencyRepository extends JpaRepository<HrContractorAgency, Long> {
    List<HrContractorAgency> findByOrganizationIdAndIsActiveTrue(Long organizationId);
    List<HrContractorAgency> findByCommunityId(Long communityId);
    Optional<HrContractorAgency> findByAgencyCode(String agencyCode);
}
