package com.manacommunity.api.socialcircle.controller;

import com.manacommunity.api.socialcircle.dto.KittyPartyVendorPackageDtos.*;
import com.manacommunity.api.socialcircle.service.KittyPartyAuthorizationService;
import com.manacommunity.api.socialcircle.service.KittyPartyVendorPackageService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kitty-circles")
@RequiredArgsConstructor
public class KittyPartyVendorPackageController {

    private final KittyPartyVendorPackageService packageService;
    private final KittyPartyAuthorizationService authorizationService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/{circleId}/vendor-packages")
    public ResponseEntity<List<VendorPackageResponse>> getPackages(
            @PathVariable Long circleId,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        authorizationService.assertCircleMember(circleId, user.getId());
        return ResponseEntity.ok(packageService.getPackagesForCircle(circleId, category));
    }

    @PostMapping("/vendor-packages/inquire")
    public ResponseEntity<Map<String, Object>> inquirePackage(
            @Valid @RequestBody InquireVendorPackageRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(packageService.inquirePackage(user, request));
    }
}
