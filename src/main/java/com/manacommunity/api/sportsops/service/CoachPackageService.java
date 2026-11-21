package com.manacommunity.api.sportsops.service;

import com.manacommunity.api.sportsops.entity.CoachPackage;
import com.manacommunity.api.sportsops.entity.CoachPackageEnrollment;
import com.manacommunity.api.sportsops.repository.CoachPackageEnrollmentRepository;
import com.manacommunity.api.sportsops.repository.CoachPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoachPackageService {
    private final CoachPackageRepository packageRepo;
    private final CoachPackageEnrollmentRepository enrollmentRepo;

    public List<CoachPackage> listActive(Long communityId) {
        return packageRepo.findByCommunityIdAndActiveTrue(communityId);
    }

    @Transactional
    public CoachPackage create(CoachPackage pkg) {
        return packageRepo.save(pkg);
    }

    @Transactional
    public CoachPackage deactivate(Long communityId, Long id) {
        CoachPackage pkg = packageRepo.findById(id)
            .filter(p -> p.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Package not found"));
        pkg.setActive(false);
        return packageRepo.save(pkg);
    }

    @Transactional
    public CoachPackageEnrollment enroll(CoachPackageEnrollment enrollment) {
        packageRepo.findById(enrollment.getPackageId())
            .filter(p -> p.getCommunityId().equals(enrollment.getCommunityId()) && p.getActive())
            .orElseThrow(() -> new IllegalArgumentException("Package not found or inactive"));
        return enrollmentRepo.save(enrollment);
    }

    @Transactional
    public CoachPackageEnrollment recordSession(Long communityId, Long enrollmentId) {
        CoachPackageEnrollment e = enrollmentRepo.findById(enrollmentId)
            .filter(en -> en.getCommunityId().equals(communityId) && en.getActive())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        CoachPackage pkg = packageRepo.findById(e.getPackageId()).orElseThrow();
        if (e.getSessionsUsed() >= pkg.getSessions()) {
            throw new IllegalStateException("All sessions used");
        }
        e.setSessionsUsed(e.getSessionsUsed() + 1);
        if (e.getSessionsUsed() >= pkg.getSessions()) {
            e.setActive(false);
        }
        return enrollmentRepo.save(e);
    }

    public List<CoachPackageEnrollment> listEnrollments(Long communityId) {
        return enrollmentRepo.findByCommunityIdAndActiveTrue(communityId);
    }
}
