package com.manacommunity.api.social.controller;

import com.manacommunity.api.social.entity.*;
import com.manacommunity.api.social.service.SocialGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialGroupController {

    private final SocialGroupService service;

    @GetMapping("/groups")
    public ResponseEntity<Page<SocialGroup>> listGroups(
            @RequestParam Long communityId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getGroupsByCommunity(communityId, type,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PostMapping("/groups")
    public ResponseEntity<SocialGroup> createGroup(@RequestBody Map<String, Object> body) {
        Long communityId = Long.valueOf(body.get("communityId").toString());
        Long createdBy = Long.valueOf(body.get("createdBy").toString());
        String name = (String) body.get("name");
        String description = (String) body.getOrDefault("description", null);
        String groupType = (String) body.getOrDefault("groupType", "INTEREST");
        boolean isPrivate = Boolean.parseBoolean(body.getOrDefault("isPrivate", "false").toString());
        int maxMembers = Integer.parseInt(body.getOrDefault("maxMembers", "50").toString());
        return ResponseEntity.ok(service.createGroup(communityId, createdBy, name, description,
                groupType, isPrivate, maxMembers));
    }

    @GetMapping("/groups/my")
    public ResponseEntity<List<SocialGroup>> myGroups(@RequestParam Long userId) {
        return ResponseEntity.ok(service.getMyGroups(userId));
    }

    @PostMapping("/groups/{id}/join")
    public ResponseEntity<Map<String, Boolean>> join(@PathVariable Long id, @RequestParam Long userId) {
        service.joinGroup(id, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/groups/{id}/leave")
    public ResponseEntity<Map<String, Boolean>> leave(@PathVariable Long id, @RequestParam Long userId) {
        service.leaveGroup(id, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/groups/{id}/invite")
    public ResponseEntity<Map<String, Boolean>> invite(@PathVariable Long id,
            @RequestParam Long adminId, @RequestParam Long inviteeId) {
        service.inviteMember(id, adminId, inviteeId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/groups/{id}/messages")
    public ResponseEntity<Page<GroupMessage>> messages(
            @PathVariable Long id,
            @RequestParam Long requesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(service.getMessages(id, requesterId,
                PageRequest.of(page, size)));
    }

    @PostMapping("/groups/{id}/messages")
    public ResponseEntity<GroupMessage> sendMessage(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long senderId = Long.valueOf(body.get("senderId").toString());
        String content = (String) body.get("content");
        String type = (String) body.getOrDefault("type", "TEXT");
        return ResponseEntity.ok(service.sendMessage(id, senderId, content, type));
    }

    @GetMapping("/groups/{id}/events")
    public ResponseEntity<List<GroupEvent>> upcomingEvents(@PathVariable Long id) {
        return ResponseEntity.ok(service.getUpcomingEvents(id));
    }

    @PostMapping("/groups/{id}/events")
    public ResponseEntity<GroupEvent> createEvent(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long createdBy = Long.valueOf(body.get("createdBy").toString());
        String title = (String) body.get("title");
        String description = (String) body.getOrDefault("description", null);
        OffsetDateTime eventDate = OffsetDateTime.parse(body.get("eventDate").toString());
        String location = (String) body.getOrDefault("location", null);
        OffsetDateTime rsvpDeadline = body.containsKey("rsvpDeadline")
                ? OffsetDateTime.parse(body.get("rsvpDeadline").toString()) : null;
        return ResponseEntity.ok(service.createGroupEvent(id, createdBy, title, description,
                eventDate, location, rsvpDeadline));
    }

    @PostMapping("/events/{id}/rsvp")
    public ResponseEntity<Map<String, Boolean>> rsvp(@PathVariable Long id,
            @RequestParam Long userId, @RequestParam String response) {
        service.rsvpEvent(id, userId, response);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/groups/{id}/expenses")
    public ResponseEntity<List<GroupExpense>> expenses(@PathVariable Long id) {
        return ResponseEntity.ok(service.listExpenses(id));
    }

    @PostMapping("/groups/{id}/expenses")
    public ResponseEntity<GroupExpense> addExpense(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long paidBy = Long.valueOf(body.get("paidBy").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String description = (String) body.get("description");
        return ResponseEntity.ok(service.addExpense(id, paidBy, amount, description));
    }

    @PostMapping("/expenses/{id}/settle")
    public ResponseEntity<Map<String, Boolean>> settle(@PathVariable Long id,
            @RequestParam Long memberId) {
        service.settleExpense(id, memberId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/groups/{id}/balance")
    public ResponseEntity<Map<String, Object>> balance(@PathVariable Long id,
            @RequestParam Long userId) {
        BigDecimal bal = service.getMyBalance(id, userId);
        return ResponseEntity.ok(Map.of("groupId", id, "userId", userId, "balance", bal));
    }
}
