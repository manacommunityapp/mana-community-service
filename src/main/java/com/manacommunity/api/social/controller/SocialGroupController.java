package com.manacommunity.api.social.controller;

import com.manacommunity.api.social.entity.*;
import com.manacommunity.api.social.service.SocialGroupService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/social", "/api/v1/social", "/api/social-groups", "/api/v1/social-groups"})
@RequiredArgsConstructor
public class SocialGroupController {

    private final SocialGroupService service;

    private Long resolveUserId(UserPrincipal principal, Long paramUserId, Map<String, Object> body, String bodyKey) {
        if (paramUserId != null && paramUserId > 0) {
            return paramUserId;
        }
        if (body != null && bodyKey != null && body.get(bodyKey) != null) {
            return Long.valueOf(body.get(bodyKey).toString());
        }
        if (principal != null && principal.getId() != null) {
            return principal.getId();
        }
        throw new IllegalArgumentException("Authenticated user ID is required");
    }

    private Long resolveCommunityId(UserPrincipal principal, Long paramCommunityId, Map<String, Object> body) {
        if (paramCommunityId != null && paramCommunityId > 0) {
            return paramCommunityId;
        }
        if (body != null && body.get("communityId") != null) {
            long cid = Long.parseLong(body.get("communityId").toString());
            if (cid > 0) return cid;
        }
        if (principal != null && principal.getCommunityId() != null && principal.getCommunityId() > 0) {
            return principal.getCommunityId();
        }
        return 1L;
    }

    private OffsetDateTime parseDateTime(Object raw) {
        if (raw == null) return null;
        String str = raw.toString().trim();
        if (str.isEmpty()) return null;
        try {
            return OffsetDateTime.parse(str);
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(str).atOffset(ZoneOffset.UTC);
        } catch (Exception ignored) {
        }
        return LocalDateTime.parse(str).atOffset(ZoneOffset.UTC);
    }

    @GetMapping({"", "/groups"})
    public ResponseEntity<List<SocialGroup>> listGroups(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long resolvedCommunityId = resolveCommunityId(principal, communityId, null);
        Long currentUserId = principal != null ? principal.getId() : null;
        Page<SocialGroup> result = service.getGroupsByCommunity(
                resolvedCommunityId,
                type,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by("createdAt").descending()),
                currentUserId);
        return ResponseEntity.ok(result.getContent());
    }

    @PostMapping({"", "/groups"})
    public ResponseEntity<SocialGroup> createGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long communityId,
            @RequestBody Map<String, Object> body) {
        Long resolvedCommunityId = resolveCommunityId(principal, communityId, body);
        Long createdBy = resolveUserId(principal, null, body, "createdBy");
        String name = body.get("name") != null ? body.get("name").toString().trim() : "";
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Group name is required");
        }
        String description = body.get("description") != null ? body.get("description").toString() : null;
        String groupType = body.get("groupType") != null ? body.get("groupType").toString() : "INTEREST";
        boolean isPrivate = body.get("isPrivate") != null && Boolean.parseBoolean(body.get("isPrivate").toString());
        int maxMembers = body.get("maxMembers") != null ? Integer.parseInt(body.get("maxMembers").toString()) : 50;
        return ResponseEntity.ok(service.createGroup(resolvedCommunityId, createdBy, name, description,
                groupType, isPrivate, maxMembers));
    }

    @GetMapping({"/my", "/groups/my"})
    public ResponseEntity<List<SocialGroup>> myGroups(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long userId) {
        Long resolvedUserId = resolveUserId(principal, userId, null, null);
        return ResponseEntity.ok(service.getMyGroups(resolvedUserId));
    }

    @GetMapping({"/{id}", "/groups/{id}"})
    public ResponseEntity<SocialGroup> getGroup(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(service.getGroupById(id, currentUserId));
    }

    @PostMapping({"/{id}/join", "/groups/{id}/join"})
    public ResponseEntity<Map<String, Boolean>> join(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long resolvedUserId = resolveUserId(principal, userId, body, "userId");
        service.joinGroup(id, resolvedUserId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping({"/{id}/leave", "/groups/{id}/leave"})
    public ResponseEntity<Map<String, Boolean>> leave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long resolvedUserId = resolveUserId(principal, userId, body, "userId");
        service.leaveGroup(id, resolvedUserId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping({"/{id}/invite", "/groups/{id}/invite"})
    public ResponseEntity<Map<String, Boolean>> invite(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) Long inviteeId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long resolvedAdminId = resolveUserId(principal, adminId, body, "adminId");
        Long resolvedInviteeId = inviteeId != null
                ? inviteeId
                : (body != null && body.get("inviteeId") != null ? Long.valueOf(body.get("inviteeId").toString()) : null);
        if (resolvedInviteeId == null) {
            throw new IllegalArgumentException("inviteeId is required");
        }
        service.inviteMember(id, resolvedAdminId, resolvedInviteeId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping({"/{id}/messages", "/groups/{id}/messages"})
    public ResponseEntity<Page<GroupMessage>> messages(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long resolvedRequesterId = requesterId != null
                ? requesterId
                : (principal != null ? principal.getId() : null);
        return ResponseEntity.ok(service.getMessages(id, resolvedRequesterId,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100))));
    }

    @PostMapping({"/{id}/messages", "/groups/{id}/messages"})
    public ResponseEntity<GroupMessage> sendMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long senderId = resolveUserId(principal, null, body, "senderId");
        String content = body.get("content") != null ? body.get("content").toString() : "";
        String type = body.get("type") != null ? body.get("type").toString() : "TEXT";
        return ResponseEntity.ok(service.sendMessage(id, senderId, content, type));
    }

    @GetMapping({"/{id}/events", "/groups/{id}/events"})
    public ResponseEntity<List<GroupEvent>> upcomingEvents(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(service.getUpcomingEvents(id, currentUserId));
    }

    @PostMapping({"/{id}/events", "/groups/{id}/events"})
    public ResponseEntity<GroupEvent> createEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long createdBy = resolveUserId(principal, null, body, "createdBy");
        String title = body.get("title") != null ? body.get("title").toString() : "";
        String description = body.get("description") != null ? body.get("description").toString() : null;
        Object rawDate = body.get("eventDate") != null ? body.get("eventDate") : body.get("scheduledAt");
        OffsetDateTime eventDate = parseDateTime(rawDate);
        if (eventDate == null) {
            throw new IllegalArgumentException("eventDate or scheduledAt is required");
        }
        String location = body.get("location") != null ? body.get("location").toString() : null;
        OffsetDateTime rsvpDeadline = parseDateTime(body.get("rsvpDeadline"));
        return ResponseEntity.ok(service.createGroupEvent(id, createdBy, title, description,
                eventDate, location, rsvpDeadline));
    }

    @PostMapping({"/events/{id}/rsvp", "/{groupId}/events/{id}/rsvp", "/groups/{groupId}/events/{id}/rsvp"})
    public ResponseEntity<Map<String, Boolean>> rsvp(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String response,
            @RequestBody(required = false) Map<String, Object> body) {
        Long resolvedUserId = resolveUserId(principal, userId, body, "userId");
        String resolvedResponse = response;
        if ((resolvedResponse == null || resolvedResponse.isBlank()) && body != null) {
            Object statusObj = body.get("status") != null ? body.get("status") : body.get("response");
            if (statusObj != null) resolvedResponse = statusObj.toString();
        }
        if (resolvedResponse == null || resolvedResponse.isBlank()) {
            throw new IllegalArgumentException("RSVP response/status is required");
        }
        service.rsvpEvent(id, resolvedUserId, resolvedResponse);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping({"/{id}/expenses", "/groups/{id}/expenses"})
    public ResponseEntity<List<GroupExpense>> expenses(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(service.listExpenses(id, currentUserId));
    }

    @PostMapping({"/{id}/expenses", "/groups/{id}/expenses"})
    public ResponseEntity<GroupExpense> addExpense(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long paidBy = resolveUserId(principal, null, body, "paidBy");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String description = body.get("description") != null ? body.get("description").toString() : "";
        return ResponseEntity.ok(service.addExpense(id, paidBy, amount, description));
    }

    @PostMapping({"/expenses/{id}/settle", "/{groupId}/expenses/{id}/settle", "/groups/{groupId}/expenses/{id}/settle"})
    public ResponseEntity<Map<String, Boolean>> settle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long memberId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long resolvedMemberId = resolveUserId(principal, memberId, body, "memberId");
        service.settleExpense(id, resolvedMemberId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping({"/{id}/balance", "/groups/{id}/balance"})
    public ResponseEntity<Map<String, Object>> balance(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long userId) {
        Long resolvedUserId = resolveUserId(principal, userId, null, null);
        BigDecimal bal = service.getMyBalance(id, resolvedUserId);
        return ResponseEntity.ok(Map.of("groupId", id, "userId", resolvedUserId, "balance", bal));
    }
}
