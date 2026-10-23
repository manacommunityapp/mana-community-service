package com.manacommunity.api.safety.controller;

import com.manacommunity.api.safety.repository.SecurityIncidentRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/guard", "/api/security"})
@RequiredArgsConstructor
public class GuardStatsController {

    private final VisitorPassRepository visitorRepo;
    private final SecurityIncidentRepository incidentRepo;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) {
            return ResponseEntity.ok(emptyStats());
        }
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long visitorsToday = visitorRepo.countTodayVisitors(communityId, startOfDay, endOfDay);
        long pendingEntry  = visitorRepo.countByCommunityIdAndStatus(communityId, VisitorPass.PassStatus.APPROVED);
        long vehiclesIn    = visitorRepo.countVehiclesIn(communityId);
        long deliveries    = visitorRepo.countTodayDeliveries(communityId, startOfDay, endOfDay);
        long openIncidents = incidentRepo.countOpenByCommunityId(communityId);

        return ResponseEntity.ok(Map.of(
                "visitorsToday", visitorsToday,
                "pendingEntry",  pendingEntry,
                "vehiclesIn",    vehiclesIn,
                "deliveries",    deliveries,
                "openIncidents", openIncidents
        ));
    }

    @PostMapping("/alerts")
    public ResponseEntity<Map<String, Object>> raiseAlert(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String message = body.getOrDefault("message", "");
        log.info("Guard alert raised by userId={} community={}: {}",
                user.getId(),
                user.getCommunity() != null ? user.getCommunity().getId() : "none",
                message);
        return ResponseEntity.ok(Map.of(
                "status",    "SENT",
                "message",   message,
                "raisedBy",  user.getFullName(),
                "raisedAt",  LocalDateTime.now().toString()
        ));
    }

    private Map<String, Object> emptyStats() {
        return Map.of(
                "visitorsToday", 0L,
                "pendingEntry",  0L,
                "vehiclesIn",    0L,
                "deliveries",    0L,
                "openIncidents", 0L
        );
    }
}
