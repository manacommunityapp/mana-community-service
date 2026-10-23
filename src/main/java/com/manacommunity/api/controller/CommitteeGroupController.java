package com.manacommunity.api.controller;

import com.manacommunity.api.dto.CommitteeGroupRequest;
import com.manacommunity.api.dto.CommitteeGroupResponse;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.CommitteeGroup;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommitteeGroupRepository;
import com.manacommunity.api.repository.CommunityRepository;
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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/community/committee-groups")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CommitteeGroupController {

    private final CommitteeGroupRepository committeeGroupRepo;
    private final CommunityRepository communityRepo;
    private final LoggedInUserService loggedInUserService;

    /** All active committee groups for community residents. */
    @GetMapping
    public ResponseEntity<List<CommitteeGroupResponse>> getActive(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(
                committeeGroupRepo.findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAscNameAsc(communityId)
                        .stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /** Admin view — includes inactive groups. */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<CommitteeGroupResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(
                committeeGroupRepo.findByCommunityIdOrderByDisplayOrderAscNameAsc(communityId)
                        .stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /** Create a new committee group. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommitteeGroupResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommitteeGroupRequest req) {
        Long communityId = resolveCommunityId(principal);
        if (committeeGroupRepo.existsByCommunityIdAndName(communityId, req.name().trim())) {
            throw new InvalidInputException("A committee group named '" + req.name() + "' already exists.");
        }
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));
        CommitteeGroup group = CommitteeGroup.builder()
                .community(community)
                .name(req.name().trim())
                .description(req.description() != null ? req.description().trim() : null)
                .displayOrder(req.displayOrder() != null ? req.displayOrder() : 0)
                .isActive(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(committeeGroupRepo.save(group)));
    }

    /** Update an existing committee group. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommitteeGroupResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommitteeGroupRequest req) {
        Long communityId = resolveCommunityId(principal);
        CommitteeGroup group = committeeGroupRepo.findByCommunityIdAndId(communityId, id)
                .orElseThrow(() -> new ResourceNotFoundException("CommitteeGroup", id));
        if (!group.getName().equalsIgnoreCase(req.name().trim()) &&
                committeeGroupRepo.existsByCommunityIdAndName(communityId, req.name().trim())) {
            throw new InvalidInputException("A committee group named '" + req.name() + "' already exists.");
        }
        group.setName(req.name().trim());
        group.setDescription(req.description() != null ? req.description().trim() : null);
        if (req.displayOrder() != null) {
            group.setDisplayOrder(req.displayOrder());
        }
        return ResponseEntity.ok(toResponse(committeeGroupRepo.save(group)));
    }

    /** Toggle active / inactive status. */
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommitteeGroupResponse> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        CommitteeGroup group = committeeGroupRepo.findByCommunityIdAndId(communityId, id)
                .orElseThrow(() -> new ResourceNotFoundException("CommitteeGroup", id));
        group.setIsActive(!Boolean.TRUE.equals(group.getIsActive()));
        return ResponseEntity.ok(toResponse(committeeGroupRepo.save(group)));
    }

    /** Delete a committee group permanently. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        CommitteeGroup group = committeeGroupRepo.findByCommunityIdAndId(communityId, id)
                .orElseThrow(() -> new ResourceNotFoundException("CommitteeGroup", id));
        committeeGroupRepo.delete(group);
        return ResponseEntity.noContent().build();
    }

    private Long resolveCommunityId(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            throw new InvalidInputException("You are not associated with any community.");
        }
        return user.getCommunity().getId();
    }

    private CommitteeGroupResponse toResponse(CommitteeGroup g) {
        return new CommitteeGroupResponse(
                g.getId(),
                g.getCommunity() != null ? g.getCommunity().getId() : null,
                g.getName(),
                g.getDescription(),
                g.getDisplayOrder(),
                g.getIsActive(),
                g.getCreatedAt(),
                g.getUpdatedAt());
    }
}
