package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.model.DomesticStaff;
import com.manacommunity.api.homeservices.model.StaffHouseholdLink;
import com.manacommunity.api.homeservices.service.StaffHouseholdService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Staff Household Links")
@RestController
@RequestMapping("/api/staff/household")
@RequiredArgsConstructor
public class StaffHouseholdController {

    private final StaffHouseholdService service;

    @PostMapping("/link")
    public ResponseEntity<ApiResponse<StaffHouseholdLink>> link(
            @RequestBody LinkRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        StaffHouseholdLink link = service.linkToHousehold(
                req.communityId, req.staffId, principal.getId(),
                req.flatNumber, req.schedule);
        return ResponseEntity.ok(ApiResponse.ok(link));
    }

    @DeleteMapping("/link/{linkId}")
    public ResponseEntity<ApiResponse<Void>> unlink(
            @PathVariable Long linkId,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.unlink(linkId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/my-staff")
    public ResponseEntity<ApiResponse<List<DomesticStaff>>> myStaff(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.getMyStaff(principal.getId())));
    }

    @Data
    public static class LinkRequest {
        Long communityId;
        Long staffId;
        String flatNumber;
        String schedule;
    }
}
