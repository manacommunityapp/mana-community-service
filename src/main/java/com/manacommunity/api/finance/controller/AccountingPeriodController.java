package com.manacommunity.api.finance.controller;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunitySettings;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.CommunitySettingsRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/accounting-periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final CommunitySettingsRepository settingsRepo;
    private final CommunityRepository communityRepo;

    @GetMapping("/freeze")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> getFreeze(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        Map<String, Object> result = new HashMap<>();
        settingsRepo.findByCommunityId(communityId).ifPresent(s -> {
            result.put("freezeDate", s.getAccountingFreezeDate());
            result.put("freezeNote", s.getAccountingFreezeNote());
        });
        if (!result.containsKey("freezeDate")) {
            result.put("freezeDate", null);
            result.put("freezeNote", null);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/freeze")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> setFreeze(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        Long communityId = principal.getCommunityId();
        CommunitySettings settings = settingsRepo.findByCommunityId(communityId)
                .orElseGet(() -> {
                    Community community = communityRepo.findById(communityId)
                            .orElseThrow(() -> new IllegalArgumentException("Community not found"));
                    return CommunitySettings.builder().community(community).build();
                });
        settings.setAccountingFreezeDate(LocalDate.parse(body.get("freezeDate")));
        settings.setAccountingFreezeNote(body.get("freezeNote"));
        settingsRepo.save(settings);
        Map<String, Object> result = new HashMap<>();
        result.put("freezeDate", settings.getAccountingFreezeDate());
        result.put("freezeNote", settings.getAccountingFreezeNote());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/freeze")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Void> clearFreeze(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        settingsRepo.findByCommunityId(communityId).ifPresent(s -> {
            s.setAccountingFreezeDate(null);
            s.setAccountingFreezeNote(null);
            settingsRepo.save(s);
        });
        return ResponseEntity.noContent().build();
    }
}
