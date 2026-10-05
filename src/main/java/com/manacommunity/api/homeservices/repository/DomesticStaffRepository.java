package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.model.DomesticStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DomesticStaffRepository extends JpaRepository<DomesticStaff, Long> {

    List<DomesticStaff> findByCommunityIdOrderByNameAsc(Long communityId);

    List<DomesticStaff> findByCommunityIdAndRoleOrderByNameAsc(Long communityId, DomesticStaff.StaffRole role);
}
