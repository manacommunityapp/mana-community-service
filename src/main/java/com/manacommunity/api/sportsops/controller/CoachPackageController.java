package com.manacommunity.api.sportsops.controller;

import com.manacommunity.api.sportsops.entity.CoachPackage;
import com.manacommunity.api.sportsops.entity.CoachPackageEnrollment;
import com.manacommunity.api.sportsops.service.CoachPackageService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sports-ops/coach-packages")
@RequiredArgsConstructor
public class CoachPackageController {
    private final CoachPackageService service;

    @GetMapping
    public List<CoachPackage> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listActive(principal.getCommunityId());
    }

    @PostMapping
    public CoachPackage create(@AuthenticationPrincipal UserPrincipal principal,
                                @RequestBody CoachPackage pkg) {
        pkg.setCommunityId(principal.getCommunityId());
        return service.create(pkg);
    }

    @PutMapping("/{id}/deactivate")
    public CoachPackage deactivate(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long id) {
        return service.deactivate(principal.getCommunityId(), id);
    }

    @PostMapping("/{packageId}/enroll")
    public CoachPackageEnrollment enroll(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable Long packageId) {
        CoachPackageEnrollment e = new CoachPackageEnrollment();
        e.setCommunityId(principal.getCommunityId());
        e.setPackageId(packageId);
        e.setUserId(principal.getUserId());
        return service.enroll(e);
    }

    @PutMapping("/enrollments/{enrollmentId}/record-session")
    public CoachPackageEnrollment recordSession(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable Long enrollmentId) {
        return service.recordSession(principal.getCommunityId(), enrollmentId);
    }

    @GetMapping("/enrollments")
    public List<CoachPackageEnrollment> enrollments(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listEnrollments(principal.getCommunityId());
    }
}
