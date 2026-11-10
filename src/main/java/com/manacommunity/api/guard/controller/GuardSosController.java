package com.manacommunity.api.guard.controller;

import com.manacommunity.api.guard.entity.GuardSos;
import com.manacommunity.api.guard.service.GuardSosService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guard/sos")
@RequiredArgsConstructor
public class GuardSosController {

    private final GuardSosService guardSosService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    public ResponseEntity<GuardSos> triggerSos(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SosRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        GuardSos sos = guardSosService.triggerSos(
                caller.getCommunity().getId(), caller.getId(),
                request.getLatitude(), request.getLongitude(), request.getMessage());
        return ResponseEntity.status(HttpStatus.CREATED).body(sos);
    }

    @GetMapping("/active")
    public ResponseEntity<List<GuardSos>> getActiveSos(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(guardSosService.getActiveSos(caller.getCommunity().getId()));
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<GuardSos> acknowledgeSos(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(guardSosService.acknowledgeSos(id, caller.getId()));
    }

    @Data
    public static class SosRequest {
        private Double latitude;
        private Double longitude;
        private String message;
    }
}
