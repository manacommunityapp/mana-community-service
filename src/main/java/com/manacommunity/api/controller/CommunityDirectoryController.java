package com.manacommunity.api.controller;

import com.manacommunity.api.dto.CommunityLeaderRequest;
import com.manacommunity.api.dto.CommunityLeaderResponse;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.CommunityLeader;
import com.manacommunity.api.repository.CommunityLeaderRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** REST controller for the Community Directory (committee leaders / office-bearers). */
@RestController
@RequestMapping("/api/community/directory")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CommunityDirectoryController {

    private final CommunityLeaderRepository leaderRepo;
    private final CommunityRepository communityRepo;
    private final AppUserRepository userRepo;
    private final LoggedInUserService loggedInUserService;

    /** Active leaders for community residents. */
    @GetMapping
    public ResponseEntity<List<CommunityLeaderResponse>> getActive(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(leaderRepo
                .findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(communityId)
                .stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /** Admin view including inactive leaders. */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<CommunityLeaderResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(leaderRepo
                .findByCommunityIdOrderByDisplayOrderAsc(communityId)
                .stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /** Single leader by id. */
    @GetMapping("/{id}")
    public ResponseEntity<CommunityLeaderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(leaderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityLeader", id))));
    }

    /** Distinct designation strings used in this community. */
    @GetMapping("/designations")
    public ResponseEntity<List<Map<String, Object>>> getDesignations(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        List<Map<String, Object>> designations = leaderRepo
                .findByCommunityIdOrderByDisplayOrderAsc(communityId)
                .stream()
                .map(CommunityLeader::getDesignation)
                .filter(d -> d != null && !d.isBlank())
                .distinct().sorted()
                .map(d -> Map.<String, Object>of("id", d, "name", d, "communityId", communityId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(designations);
    }

    /** Echo designation name back (designations stored inline, no separate table). */
    @PostMapping("/designations")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Map<String, Object>> addDesignation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        Long communityId = resolveCommunityId(principal);
        String name = body.getOrDefault("name", "").trim();
        if (name.isBlank()) { throw new InvalidInputException("Designation name must not be blank."); }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", name, "name", name, "communityId", communityId));
    }

    /** Add a community leader / office-bearer. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityLeaderResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommunityLeaderRequest req) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        if (currentUser.getCommunity() == null) { throw new InvalidInputException("You are not associated with any community."); }
        Long communityId = currentUser.getCommunity().getId();
        if (leaderRepo.existsByCommunityIdAndUserIdAndDesignation(communityId, req.userId(), req.designation())) {
            throw new InvalidInputException("This user already holds the designation '" + req.designation() + "' in your community.");
        }
        AppUser targetUser = userRepo.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", req.userId()));
        com.manacommunity.api.model.Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));
        CommunityLeader leader = CommunityLeader.builder()
                .community(community).user(targetUser).designation(req.designation())
                .committee(req.committee()).contactPhone(req.contactPhone())
                .contactEmail(req.contactEmail())
                .displayOrder(req.displayOrder() != null ? req.displayOrder() : 0)
                .isActive(true).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(leaderRepo.save(leader)));
    }

    /** Update an existing leader entry. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityLeaderResponse> update(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommunityLeaderRequest req) {
        CommunityLeader leader = leaderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityLeader", id));
        AppUser targetUser = userRepo.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", req.userId()));
        leader.setUser(targetUser); leader.setDesignation(req.designation());
        leader.setCommittee(req.committee()); leader.setContactPhone(req.contactPhone());
        leader.setContactEmail(req.contactEmail());
        if (req.displayOrder() != null) { leader.setDisplayOrder(req.displayOrder()); }
        return ResponseEntity.ok(toResponse(leaderRepo.save(leader)));
    }

    /** Remove a leader entry permanently. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        leaderRepo.delete(leaderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityLeader", id)));
        return ResponseEntity.noContent().build();
    }

    /** Toggle active / inactive status. */
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityLeaderResponse> toggleStatus(@PathVariable Long id) {
        CommunityLeader leader = leaderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityLeader", id));
        leader.setIsActive(!Boolean.TRUE.equals(leader.getIsActive()));
        return ResponseEntity.ok(toResponse(leaderRepo.save(leader)));
    }

    private Long resolveCommunityId(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) { throw new InvalidInputException("You are not associated with any community."); }
        return user.getCommunity().getId();
    }

    private CommunityLeaderResponse toResponse(CommunityLeader l) {
        AppUser u = l.getUser();
        return new CommunityLeaderResponse(
                l.getId(),
                u != null ? u.getId() : null,
                u != null ? u.getFullName() : null,
                u != null ? u.getProfilePicUrl() : null,
                l.getDesignation(), l.getCommittee(), l.getContactPhone(), l.getContactEmail(),
                u != null ? u.getFlatNo() : null,
                u != null ? u.getBlock() : null,
                l.getDisplayOrder());
    }
}