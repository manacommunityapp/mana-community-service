package com.manacommunity.api.securityops.controller;

import com.manacommunity.api.securityops.dto.SecurityRecordRequest;
import com.manacommunity.api.securityops.dto.SecurityRecordResponse;
import com.manacommunity.api.securityops.service.CommunitySecurityRecordService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/security") @RequiredArgsConstructor
public class CommunitySecurityRecordController {
    private final CommunitySecurityRecordService service;
    private final LoggedInUserService loggedInUserService;
    private AppUser user(UserPrincipal p) { return loggedInUserService.resolve(p); }

    @GetMapping("/trusted-visitors") public List<SecurityRecordResponse> trusted(@AuthenticationPrincipal UserPrincipal p) { return service.list(user(p), "TRUSTED_VISITOR"); }
    @PostMapping("/trusted-visitors") public ResponseEntity<SecurityRecordResponse> createTrusted(@AuthenticationPrincipal UserPrincipal p, @RequestBody SecurityRecordRequest q) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user(p), "TRUSTED_VISITOR", q, true, false)); }
    @PutMapping("/trusted-visitors/{id}") public SecurityRecordResponse updateTrusted(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id, @RequestBody SecurityRecordRequest q) { return service.update(user(p), id, "TRUSTED_VISITOR", q); }
    @DeleteMapping("/trusted-visitors/{id}") public ResponseEntity<Void> deleteTrusted(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) { service.delete(user(p), id, "TRUSTED_VISITOR"); return ResponseEntity.noContent().build(); }

    @GetMapping("/packages") public List<SecurityRecordResponse> packages(@AuthenticationPrincipal UserPrincipal p) { return service.list(user(p), "PACKAGE"); }
    @PostMapping("/packages") public ResponseEntity<SecurityRecordResponse> createPackage(@AuthenticationPrincipal UserPrincipal p, @RequestBody SecurityRecordRequest q) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user(p), "PACKAGE", q, true, true)); }
    @PostMapping("/packages/{id}/collect") public SecurityRecordResponse collect(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id, @RequestParam String otp) { return service.collect(user(p), id, otp); }

    @GetMapping("/blacklist") public List<SecurityRecordResponse> blacklist(@AuthenticationPrincipal UserPrincipal p) { return service.list(user(p), "BLACKLIST"); }
    @PostMapping("/blacklist") public ResponseEntity<SecurityRecordResponse> createBlacklist(@AuthenticationPrincipal UserPrincipal p, @RequestBody SecurityRecordRequest q) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user(p), "BLACKLIST", q, false, false)); }
    @PutMapping("/blacklist/{id}") public SecurityRecordResponse updateBlacklist(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id, @RequestBody SecurityRecordRequest q) { return service.update(user(p), id, "BLACKLIST", q); }
    @DeleteMapping("/blacklist/{id}") public ResponseEntity<Void> deleteBlacklist(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) { service.delete(user(p), id, "BLACKLIST"); return ResponseEntity.noContent().build(); }

    @PostMapping("/intercom/calls") public ResponseEntity<SecurityRecordResponse> call(@AuthenticationPrincipal UserPrincipal p, @RequestBody SecurityRecordRequest q) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user(p), "INTERCOM_CALL", q, true, false)); }
    @GetMapping("/intercom/calls") public List<SecurityRecordResponse> calls(@AuthenticationPrincipal UserPrincipal p) { return service.list(user(p), "INTERCOM_CALL"); }
    @PutMapping("/intercom/calls/{id}") public SecurityRecordResponse decide(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id, @RequestBody SecurityRecordRequest q) { return service.update(user(p), id, "INTERCOM_CALL", q); }

    @GetMapping("/guard/logs") public List<SecurityRecordResponse> guardLogs(@AuthenticationPrincipal UserPrincipal p) { return service.list(user(p), "GUARD_DAILY_LOG"); }
    @PostMapping("/guard/logs") public ResponseEntity<SecurityRecordResponse> guardLog(@AuthenticationPrincipal UserPrincipal p, @RequestBody SecurityRecordRequest q) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user(p), "GUARD_DAILY_LOG", q, false, false)); }
    @GetMapping("/guard/handover") public List<SecurityRecordResponse> handovers(@AuthenticationPrincipal UserPrincipal p) { return service.list(user(p), "SHIFT_HANDOVER"); }
    @PostMapping("/guard/handover") public ResponseEntity<SecurityRecordResponse> handover(@AuthenticationPrincipal UserPrincipal p, @RequestBody SecurityRecordRequest q) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user(p), "SHIFT_HANDOVER", q, false, false)); }
}
