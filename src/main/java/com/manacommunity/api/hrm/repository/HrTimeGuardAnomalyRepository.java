package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrTimeGuardAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrTimeGuardAnomalyRepository extends JpaRepository<HrTimeGuardAnomaly, String> {
    List<HrTimeGuardAnomaly> findByOrganizationIdOrderByEventTimestampDesc(Long organizationId);
    List<HrTimeGuardAnomaly> findByCommunityIdOrderByEventTimestampDesc(Long communityId);
    List<HrTimeGuardAnomaly> findByOrganizationIdAndStatus(Long organizationId, String status);
}
