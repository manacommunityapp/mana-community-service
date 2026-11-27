package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.CoachPackageEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CoachPackageEnrollmentRepository extends JpaRepository<CoachPackageEnrollment, Long> {
    List<CoachPackageEnrollment> findByPackageId(Long packageId);
    List<CoachPackageEnrollment> findByCommunityIdAndUserId(Long communityId, Long userId);
    List<CoachPackageEnrollment> findByCommunityIdAndActiveTrue(Long communityId);
}
