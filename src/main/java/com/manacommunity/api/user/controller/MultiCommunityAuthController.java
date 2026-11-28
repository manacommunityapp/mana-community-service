package com.manacommunity.api.user.controller;

import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.CommunityModule;
import com.manacommunity.api.repository.CommunityModuleRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.security.CommunityMembershipService;
import com.manacommunity.api.security.CommunityMembershipService.CommunityRoleContext;
import com.manacommunity.api.security.CookieAuthHelper;
import com.manacommunity.api.security.JwtTokenProvider;
import com.manacommunity.api.service.RolePermissionService;
import com.manacommunity.api.user.dto.AuthResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Exposes multi-community membership discovery and server-authorized community context switching.
 * Allows one authenticated user to switch seamlessly across communities where they hold
 * distinct roles (e.g. Owner in Community A, Tenant in Community B, Vendor in Community C).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MultiCommunityAuthController {

    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMembershipService membershipService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RolePermissionService rolePermissionService;
    private final CommunityModuleRepository communityModuleRepository;
    private final CookieAuthHelper cookieAuthHelper;

    public record SwitchCommunityRequest(Long communityId, Long flatId) {
        public SwitchCommunityRequest(Long communityId) {
            this(communityId, null);
        }
    }

    @GetMapping("/communities")
    public ResponseEntity<Map<String, Object>> getMyCommunities(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new UnauthorizedActionException("Authentication is required.");
        }
        AppUser user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedActionException("User not found."));

        List<CommunityRoleContext> contexts = membershipService.getCommunityContexts(user);
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "activeCommunityId", principal.getCommunityId() != null ? principal.getCommunityId() : 0L,
                "communities", contexts
        ));
    }

    @PostMapping("/switch-community")
    public ResponseEntity<AuthResponse> switchCommunity(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SwitchCommunityRequest request,
            HttpServletResponse httpResponse) {
        if (principal == null || principal.getId() == null) {
            throw new UnauthorizedActionException("Authentication is required.");
        }
        if (request == null || request.communityId() == null) {
            throw new IllegalArgumentException("target communityId is required.");
        }
        AppUser user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedActionException("User not found."));

        Long targetCommunityId = request.communityId();
        if (!membershipService.isUserAuthorizedForCommunity(user, targetCommunityId)) {
            throw new AccessDeniedException("User is not a verified resident, owner, tenant, or vendor in community " + targetCommunityId);
        }

        if (communityRepository != null) {
            communityRepository.findById(targetCommunityId).ifPresent(comm -> {
                user.setCommunity(comm);
                userRepository.save(user);
            });
        }

        List<String> scopedRoles = membershipService.resolveRolesForCommunity(user, targetCommunityId);
        List<String> effectivePerms = rolePermissionService.getEffectivePermissionsForUser(user);

        CommunityMembershipService.ResidentUnitContext unit = null;
        if (request.flatId() != null) {
            if (!membershipService.isUserAuthorizedForProperty(user, targetCommunityId, request.flatId())) {
                throw new AccessDeniedException("User is not authorized for flat " + request.flatId()
                        + " in community " + targetCommunityId);
            }
            unit = membershipService.resolveUnitContextForFlat(user, targetCommunityId, request.flatId());
        } else {
            unit = membershipService.resolveDefaultUnitContext(user, targetCommunityId);
        }

        Long towerId = unit != null ? unit.towerId() : CommunityMembershipService.computeTowerId(
                targetCommunityId,
                (user.getTower() != null && !user.getTower().isBlank()) ? user.getTower() : user.getBlock());
        Long flatId = unit != null ? unit.flatId() : request.flatId();
        String towerName = unit != null && unit.towerBlock() != null ? unit.towerBlock() : user.getTower();
        String flatNo = unit != null && unit.flatNumber() != null ? unit.flatNumber() : user.getFlatNo();

        String accessToken = jwtTokenProvider.generateTokenForContext(
                user, targetCommunityId, towerId, flatId, scopedRoles, effectivePerms);
        String refreshToken = jwtTokenProvider.generateRefreshTokenForContext(
                user, targetCommunityId, towerId, flatId, scopedRoles);

        if (httpResponse != null && cookieAuthHelper != null) {
            cookieAuthHelper.setAuthCookies(httpResponse, accessToken, refreshToken);
        }

        List<String> enabledModules = communityModuleRepository.findEnabledByCommunityId(targetCommunityId)
                .stream()
                .map(CommunityModule::getModuleKey)
                .toList();

        AuthResponse response = new AuthResponse(
                String.valueOf(user.getId()),
                "Switched active community context",
                accessToken,
                user.getFullName(),
                user.getEmail(),
                String.join(",", scopedRoles),
                targetCommunityId,
                user.getDateOfBirth()
        );
        response.setTowerId(towerId);
        response.setFlatId(flatId);
        response.setTower(towerName);
        response.setBlock(user.getBlock());
        response.setFlatNo(flatNo);
        response.setRefreshToken(refreshToken);
        response.setRoles(scopedRoles);
        response.setPermissions(effectivePerms);
        response.setEnabledModules(enabledModules);
        return ResponseEntity.ok(response);
    }
}
