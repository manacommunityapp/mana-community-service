package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.model.StaffHouseholdLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffHouseholdLinkRepository extends JpaRepository<StaffHouseholdLink, Long> {

    List<StaffHouseholdLink> findByResidentUserIdAndActiveTrue(Long residentUserId);

    List<StaffHouseholdLink> findByCommunityIdAndActiveTrue(Long communityId);

    Optional<StaffHouseholdLink> findByStaffIdAndResidentUserIdAndActiveTrue(Long staffId, Long residentUserId);

    List<StaffHouseholdLink> findByStaffIdAndActiveTrue(Long staffId);
}
