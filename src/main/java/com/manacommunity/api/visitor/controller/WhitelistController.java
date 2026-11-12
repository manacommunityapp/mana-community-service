package com.manacommunity.api.visitor.controller;

import com.manacommunity.api.visitor.dto.WhitelistRequest;
import com.manacommunity.api.visitor.entity.WhitelistEntry;
import com.manacommunity.api.visitor.service.WhitelistService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visitors/whitelist")
@RequiredArgsConstructor
public class WhitelistController {

    private final WhitelistService whitelistService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<WhitelistEntry>> get(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(whitelistService.getMyWhitelist(loggedInUserService.resolve(principal)));
    }

    @PostMapping
    public ResponseEntity<WhitelistEntry> add(@RequestBody WhitelistRequest req,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(whitelistService.add(req, loggedInUserService.resolve(principal)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WhitelistEntry> update(@PathVariable Long id,
                                                  @RequestBody WhitelistRequest req,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(whitelistService.update(id, req, loggedInUserService.resolve(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        whitelistService.remove(id, loggedInUserService.resolve(principal));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WhitelistEntry> toggleStatus(@PathVariable Long id,
                                                        @RequestBody Map<String, Boolean> body,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(whitelistService.toggleStatus(id, Boolean.TRUE.equals(body.get("isActive")), user));
    }
}
