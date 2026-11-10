package com.manacommunity.api.mobile;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Announcement;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.AnnouncementRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mobile-facing broadcast API at /api/admin/broadcasts.
 * Maps the Announcement entity to the Broadcast shape the mobile app expects.
 */
@RestController
@RequestMapping("/api/admin/broadcasts")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class BroadcastController {

    private final AnnouncementRepository announcementRepo;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getBroadcastHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "20") int limit) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = resolveCommunityId(user);
        Pageable page = PageRequest.of(0, Math.min(limit, 50));
        List<Map<String, Object>> results = announcementRepo
                .findByCommunityIdOrderByPinnedDescCreatedAtDesc(communityId, page)
                .getContent()
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN','MODERATOR')")
    public ResponseEntity<Map<String, Object>> sendBroadcast(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        AppUser user = loggedInUserService.resolve(principal);
        Community community = user.getCommunity();
        if (community == null) return ResponseEntity.badRequest().build();

        String priority = mapPriorityToAnnouncement(body.getOrDefault("priority", "MEDIUM"));

        Announcement ann = Announcement.builder()
                .community(community)
                .author(user)
                .title(body.getOrDefault("title", "").trim())
                .content(body.getOrDefault("message", body.getOrDefault("content", "")).trim())
                .priority(priority)
                .pinned(false)
                .build();

        ann = announcementRepo.save(ann);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(ann));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN','MODERATOR')")
    public ResponseEntity<Void> deleteBroadcast(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = resolveCommunityId(user);
        Announcement ann = announcementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Broadcast", id));
        if (!ann.getCommunity().getId().equals(communityId)) {
            throw new UnauthorizedActionException("Broadcast does not belong to your community.");
        }
        announcementRepo.delete(ann);
        return ResponseEntity.noContent().build();
    }

    private Long resolveCommunityId(AppUser user) {
        if (user.getCommunity() == null) {
            throw new UnauthorizedActionException("You are not associated with any community.");
        }
        return user.getCommunity().getId();
    }

    private String mapPriorityToAnnouncement(String mobilePriority) {
        return switch (mobilePriority.toUpperCase()) {
            case "CRITICAL", "HIGH" -> "URGENT";
            default -> "NORMAL";
        };
    }

    private Map<String, Object> toResponse(Announcement a) {
        String announcementPriority = a.getPriority();
        String mobilePriority = "URGENT".equals(announcementPriority) ? "HIGH" : "MEDIUM";

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         a.getId());
        m.put("title",      a.getTitle());
        m.put("message",    a.getContent());
        m.put("category",   "GENERAL");
        m.put("priority",   mobilePriority);
        m.put("target",     "ALL");
        m.put("sentAt",     a.getCreatedAt() != null ? a.getCreatedAt().toString() : LocalDateTime.now().toString());
        m.put("status",     "SENT");
        m.put("sentBy",     a.getAuthor() != null ? a.getAuthor().getFullName() : "Admin");
        m.put("reachCount", 0);
        return m;
    }
}
