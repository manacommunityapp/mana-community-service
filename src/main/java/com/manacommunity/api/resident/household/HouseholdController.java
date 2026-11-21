package com.manacommunity.api.resident.household;

import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/households")
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdService householdService;
    private final LoggedInUserService loggedInUserService;

    /** Full household graph for the current user (members, dependents, vehicles, staff). */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<HouseholdResponse>> getMyHousehold(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HouseholdResponse response = householdService.getMyHousehold(user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** Flat by ID (own flat or FM/ADMIN access). */
    @GetMapping("/{flatId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<HouseholdResponse>> getHousehold(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long flatId) {
        AppUser user = loggedInUserService.resolve(principal);
        HouseholdResponse response = householdService.getHousehold(flatId, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** Add home help staff to the caller's flat. */
    @PostMapping("/{flatId}/staff")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<HouseholdResponse.StaffDto>> addStaff(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long flatId,
            @Valid @RequestBody StaffRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        HouseholdStaffLink link = householdService.addStaff(flatId, request, user);
        HouseholdResponse.StaffDto dto = HouseholdResponse.StaffDto.builder()
                .linkId(link.getId())
                .staffName(link.getStaffName())
                .staffType(link.getStaffType())
                .phone(link.getPhone())
                .sinceDate(link.getSinceDate())
                .active(link.getActive())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto, "Staff added"));
    }

    /** Soft-remove a staff link. */
    @DeleteMapping("/staff/{linkId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removeStaff(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long linkId) {
        AppUser user = loggedInUserService.resolve(principal);
        householdService.removeStaff(linkId, user);
        return ResponseEntity.ok(ApiResponse.ok(null, "Staff removed"));
    }
}
