package com.manacommunity.api.mobile.schoolbus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolBusRepository extends JpaRepository<SchoolBus, Long> {
    List<SchoolBus> findByCommunityIdOrderBySchoolName(Long communityId);
    List<SchoolBus> findByCommunityIdAndSchoolNameContainingIgnoreCase(Long communityId, String schoolName);
}
