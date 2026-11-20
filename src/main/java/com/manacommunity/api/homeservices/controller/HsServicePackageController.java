package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.entity.HsServicePackage;
import com.manacommunity.api.homeservices.repository.HsServicePackageRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home-services/packages")
@RequiredArgsConstructor
public class HsServicePackageController {

    private final HsServicePackageRepository packageRepo;

    @GetMapping
    public ResponseEntity<List<HsServicePackage>> list(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) String category) {
        if (category != null)
            return ResponseEntity.ok(packageRepo.findByCommunityIdAndCategoryAndIsActiveTrue(p.getCommunityId(), category));
        return ResponseEntity.ok(packageRepo.findByCommunityIdAndIsActiveTrue(p.getCommunityId()));
    }

    @PostMapping
    public ResponseEntity<HsServicePackage> create(@RequestBody HsServicePackage pkg,
            @AuthenticationPrincipal UserPrincipal p) {
        pkg.setCommunityId(p.getCommunityId());
        return ResponseEntity.ok(packageRepo.save(pkg));
    }
}
