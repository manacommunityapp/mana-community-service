package com.manacommunity.api.controller;

import com.manacommunity.api.dto.CommunityLeaderRequest;
import com.manacommunity.api.dto.CommunityLeaderResponse;
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

@RestController
@RequestMapping("/api/community/directory")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CommunityDirectoryController {

    private final CommunityLeaderRepository leaderRepo;
    private final CommunityRepository communityRepo;
    private final AppUserRepository userRepo;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<CommunityLeaderResponse>> getDirectory(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        List<CommunityLeaderResponse> leaders = leaderRepo
                .findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(communityId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(leaders);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityLeaderResponse> addLeader(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommunityLeaderRequest req) {
        AppUser admin = loggedInUserService.resolve(principal);
        Long communityId = admin.getCommunity().getId();

        AppUser targetUser = userRepo.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.userId().toString()));

        CommunityLeader leader = CommunityLeader.builder()
                .community(communityRepo.findById(communityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId.toString())))
                .user(targetUser)
                .designation(req.designation())
                .committee(req.committee())
                .contactPhone(req.contactPhone() != null ? req.contactPhone() : targetUser.getPhone())
                .contactEmail(req.contactEmail() != null ? req.contactEmail() : targetUser.getEmail())
                .displayOrder(req.displayOrder() != null ? req.displayOrder() : 0)
                .build();

        leader = leaderRepo.save(leader);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(leader));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityLeaderResponse> updateLeader(
            @PathVariable Long id,
            @Valid @RequestBody CommunityLeaderRequest req) {
        CommunityLeader leader = leaderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityLeader", "id", id.toString()));

        leader.setDesignation(req.designation());
        leader.setCommittee(req.committee());
        leader.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : leader.getDisplayOrder());

        if (req.contactPhone() != null) leader.setContactPhone(req.contactPhone());
        if (req.contactEmail() != null) leader.setContactEmail(req.contactEmail());

        leader = leaderRepo.save(leader);
        return ResponseEntity.ok(toResponse(leader));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> removeLeader(@PathVariable Long id) {
        CommunityLeader leader = leaderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityLeader", "id", id.toString()));
        leader.setIsActive(false);
        leaderRepo.save(leader);
        return ResponseEntity.noContent().build();
    }

    private CommunityLeaderResponse toResponse(CommunityLeader l) {
        AppUser u = l.getUser();
        return new CommunityLeaderResponse(
                l.getId(),
                u.getId(),
                u.getFullName(),
                u.getProfilePicUrl(),
                l.getDesignation(),
                l.getCommittee(),
                l.getContactPhone(),
                l.getContactEmail(),
                u.getFlatNo(),
                u.getBlock(),
                l.getDisplayOrder()
        );
    }
}
