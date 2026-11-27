package com.manacommunity.api.resident.household;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HouseholdStaffLinkRepository extends JpaRepository<HouseholdStaffLink, Long> {

    List<HouseholdStaffLink> findByFlatIdAndActiveTrue(Long flatId);

    List<HouseholdStaffLink> findByFlatId(Long flatId);
}
