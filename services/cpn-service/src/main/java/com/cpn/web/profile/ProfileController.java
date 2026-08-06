package com.cpn.web.profile;

import com.cpn.application.profile.dto.ProfileDto;
import com.cpn.application.profile.service.ProfileService;
import com.cpn.infrastructure.multitenancy.TenantContext;
import com.cpn.infrastructure.security.CustomUserDetails;
import com.cpn.web.common.ApiResponse;
import com.cpn.web.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "Profile APIs")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ApiResponse<ProfileDto> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(profileService.getProfile(userDetails.getUserId(), userDetails.getTenantId()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search profiles")
    public ApiResponse<PageResponse<ProfileDto>> searchProfiles(
            @RequestParam(required = false) String skill,
            Pageable pageable) {
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        return ApiResponse.success(PageResponse.of(profileService.searchProfiles(skill, tenantId, pageable)));
    }
}
