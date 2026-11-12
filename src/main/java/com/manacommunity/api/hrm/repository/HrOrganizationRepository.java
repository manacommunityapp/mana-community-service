package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrOrganization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HrOrganizationRepository extends JpaRepository<HrOrganization, Long> {

    List<HrOrganization> findByCommunityId(Long communityId);

    Optional<HrOrganization> findByIdAndCommunityId(Long id, Long communityId);
}
