package com.manacommunity.api.visitor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.visitor.entity.VisitorBlacklist;
import com.manacommunity.api.visitor.repository.VisitorBlacklistRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visitors/blacklist")
@RequiredArgsConstructor
public class VisitorBlacklistController {

    private final VisitorBlacklistRepository repo;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<VisitorBlacklist>> getBlacklist(
            @RequestParam Long communityId) {
        return ResponseEntity.ok(repo.findByCommunityIdAndIsActiveTrueOrderByCreatedAtDesc(communityId));
    }

    @PostMapping
    public ResponseEntity<VisitorBlacklist> addToBlacklist(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody BlacklistRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        VisitorBlacklist entry = repo.save(VisitorBlacklist.builder()
                .communityId(caller.getCommunity().getId())
                .name(request.getName())
                .phone(request.getPhone())
                .aadhaarLast4(request.getAadhaarLast4())
                .reason(request.getReason())
                .blacklistedByUserId(caller.getId())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable Long id) {
        repo.findById(id).ifPresent(e -> {
            e.setIsActive(false);
            repo.save(e);
        });
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkBlacklist(
            @RequestParam Long communityId,
            @RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        boolean blacklisted = phone != null && repo.existsByCommunityIdAndPhoneAndIsActiveTrue(communityId, phone);
        return ResponseEntity.ok(Map.of("blacklisted", blacklisted, "phone", phone != null ? phone : ""));
    }

    @Data
    public static class BlacklistRequest {
        private String name;
        private String phone;
        private String aadhaarLast4;
        private String reason;
    }
}
