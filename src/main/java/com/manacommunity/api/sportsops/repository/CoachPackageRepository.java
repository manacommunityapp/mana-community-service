package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.CoachPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CoachPackageRepository extends JpaRepository<CoachPackage, Long> {
    List<CoachPackage> findByCommunityIdAndActiveTrue(Long communityId);
    List<CoachPackage> findByCommunityIdAndCoachId(Long communityId, Long coachId);
}
