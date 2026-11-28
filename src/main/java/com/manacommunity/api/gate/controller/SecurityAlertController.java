package com.manacommunity.api.gate.controller;

import com.manacommunity.api.gate.model.SecurityAlert;
import com.manacommunity.api.gate.repository.SecurityAlertRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/gate/alerts")
@RequiredArgsConstructor
public class SecurityAlertController {

    private final SecurityAlertRepository securityAlertRepository;

    @GetMapping("/{communityId}")
    @PreAuthorize("hasAnyAuthority('Manage Gate', 'View Gate')")
    public ResponseEntity<Page<SecurityAlert>> listAlerts(
            @PathVariable Long communityId,
            @RequestParam(required = false) SecurityAlert.AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SecurityAlert> alerts = status != null
                ? securityAlertRepository.findByCommunityIdAndAlertStatusOrderByCreatedAtDesc(
                        communityId, status, pageable)
                : securityAlertRepository.findByCommunityIdOrderByCreatedAtDesc(
                        communityId, pageable);

        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{communityId}/open/count")
    @PreAuthorize("hasAnyAuthority('Manage Gate', 'View Gate')")
    public ResponseEntity<Long> openAlertCount(@PathVariable Long communityId) {
        return ResponseEntity.ok(
                securityAlertRepository.countByCommunityIdAndAlertStatus(
                        communityId, SecurityAlert.AlertStatus.OPEN));
    }

    @PutMapping("/{communityId}/{id}/acknowledge")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<SecurityAlert> acknowledge(
            @PathVariable Long communityId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        return securityAlertRepository.findById(id)
                .filter(a -> a.getCommunityId().equals(communityId))
                .filter(a -> a.getAlertStatus() == SecurityAlert.AlertStatus.OPEN)
                .map(a -> {
                    a.setAlertStatus(SecurityAlert.AlertStatus.ACKNOWLEDGED);
                    a.setAcknowledgedBy(principal.getUsername());
                    a.setAcknowledgedAt(LocalDateTime.now());
                    return ResponseEntity.ok(securityAlertRepository.save(a));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{communityId}/{id}/resolve")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<SecurityAlert> resolve(
            @PathVariable Long communityId,
            @PathVariable Long id,
            @RequestBody ResolveRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        return securityAlertRepository.findById(id)
                .filter(a -> a.getCommunityId().equals(communityId))
                .filter(a -> a.getAlertStatus() != SecurityAlert.AlertStatus.RESOLVED)
                .map(a -> {
                    a.setAlertStatus(SecurityAlert.AlertStatus.RESOLVED);
                    a.setResolvedBy(principal.getUsername());
                    a.setResolvedAt(LocalDateTime.now());
                    a.setResolutionNotes(req.getNotes());
                    return ResponseEntity.ok(securityAlertRepository.save(a));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Data
    public static class ResolveRequest {
        @NotBlank private String notes;
    }
}
