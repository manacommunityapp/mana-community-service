package com.manacommunity.api.healthguard.controller;

import com.manacommunity.api.healthguard.dto.*;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.AmbulanceBookingStatus;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.ResponderAvailability;
import com.manacommunity.api.healthguard.service.HealthGuardService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/healthguard")
public class HealthGuardApiController {

    private final HealthGuardService healthGuardService;

    public HealthGuardApiController(HealthGuardService healthGuardService) {
        this.healthGuardService = healthGuardService;
    }

    // ── Health Profile ──

    @GetMapping("/profile")
    public ResponseEntity<HealthProfileDto> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(healthGuardService.getProfile(principal.getId()));
    }

    @PostMapping("/profile")
    public ResponseEntity<HealthProfileDto> saveProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody HealthProfileDto dto) {
        return ResponseEntity.ok(healthGuardService.saveProfile(principal.getId(), dto));
    }

    @PutMapping("/profile")
    public ResponseEntity<HealthProfileDto> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody HealthProfileDto dto) {
        return ResponseEntity.ok(healthGuardService.saveProfile(principal.getId(), dto));
    }

    // ── Emergency Contacts ──

    @GetMapping("/contacts")
    public ResponseEntity<List<EmergencyContactDto>> getContacts(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(healthGuardService.getContacts(principal.getId()));
    }

    @PostMapping("/contacts")
    public ResponseEntity<EmergencyContactDto> addContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody EmergencyContactDto dto) {
        return ResponseEntity.ok(healthGuardService.addContact(principal.getId(), dto));
    }

    @PutMapping("/contacts/{id}")
    public ResponseEntity<EmergencyContactDto> updateContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody EmergencyContactDto dto) {
        return ResponseEntity.ok(healthGuardService.updateContact(principal.getId(), id, dto));
    }

    @DeleteMapping("/contacts/{id}")
    public ResponseEntity<Void> deleteContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        healthGuardService.deleteContact(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── SOS / Incidents ──

    @PostMapping("/sos")
    public ResponseEntity<EmergencyIncidentDto> triggerSos(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody TriggerSosRequest request) {
        return ResponseEntity.ok(healthGuardService.triggerSos(principal.getId(), request));
    }

    @GetMapping("/incidents/active")
    public ResponseEntity<EmergencyIncidentDto> getActiveIncident(@AuthenticationPrincipal UserPrincipal principal) {
        EmergencyIncidentDto incident = healthGuardService.getActiveIncident(principal.getId());
        return incident != null ? ResponseEntity.ok(incident) : ResponseEntity.noContent().build();
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<EmergencyIncidentDto>> getIncidentHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(healthGuardService.getIncidentHistory(principal.getId()));
    }

    @PutMapping("/incidents/{id}/acknowledge")
    public ResponseEntity<EmergencyIncidentDto> acknowledgeIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(healthGuardService.acknowledgeIncident(principal.getId(), id));
    }

    @PutMapping("/incidents/{id}/resolve")
    public ResponseEntity<EmergencyIncidentDto> resolveIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(healthGuardService.resolveIncident(principal.getId(), id, notes));
    }

    @PutMapping("/incidents/{id}/cancel")
    public ResponseEntity<EmergencyIncidentDto> cancelIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(healthGuardService.cancelIncident(principal.getId(), id));
    }

    @PutMapping("/incidents/{id}/escalate")
    public ResponseEntity<EmergencyIncidentDto> escalateIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(healthGuardService.escalateIncident(principal.getId(), id));
    }

    // ── Community Responders ──

    @GetMapping("/responder/me")
    public ResponseEntity<CommunityResponderDto> getMyResponderProfile(@AuthenticationPrincipal UserPrincipal principal) {
        CommunityResponderDto dto = healthGuardService.getMyResponderProfile(principal.getId());
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }

    @PostMapping("/responder/register")
    public ResponseEntity<CommunityResponderDto> registerAsResponder(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CommunityResponderDto dto) {
        return ResponseEntity.ok(healthGuardService.registerAsResponder(principal.getId(), dto));
    }

    @PutMapping("/responder/status")
    public ResponseEntity<CommunityResponderDto> updateResponderStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        ResponderAvailability status = ResponderAvailability.valueOf(body.get("status"));
        return ResponseEntity.ok(healthGuardService.updateResponderStatus(principal.getId(), status));
    }

    @GetMapping("/responders/count")
    public ResponseEntity<Map<String, Long>> getAvailableRespondersCount() {
        return ResponseEntity.ok(Map.of("count", healthGuardService.countAvailableResponders()));
    }

    // ── Ambulance Booking ──

    @PostMapping("/ambulance")
    public ResponseEntity<AmbulanceBookingDto> bookAmbulance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody BookAmbulanceRequest request) {
        return ResponseEntity.ok(healthGuardService.bookAmbulance(principal.getId(), request));
    }

    @GetMapping("/ambulance/active")
    public ResponseEntity<AmbulanceBookingDto> getActiveAmbulanceBooking(@AuthenticationPrincipal UserPrincipal principal) {
        AmbulanceBookingDto dto = healthGuardService.getActiveAmbulanceBooking(principal.getId());
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }

    @GetMapping("/ambulance")
    public ResponseEntity<List<AmbulanceBookingDto>> getAmbulanceHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(healthGuardService.getAmbulanceHistory(principal.getId()));
    }

    @PutMapping("/ambulance/{id}/cancel")
    public ResponseEntity<AmbulanceBookingDto> cancelAmbulanceBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(healthGuardService.cancelAmbulanceBooking(principal.getId(), id));
    }

    // ── Admin / Responder endpoints ──

    @GetMapping("/admin/incidents")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<List<EmergencyIncidentDto>> getActiveIncidentsAdmin() {
        return ResponseEntity.ok(healthGuardService.getActiveIncidentsAdmin());
    }

    @PutMapping("/admin/ambulance/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AmbulanceBookingDto> updateAmbulanceStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        AmbulanceBookingStatus status = AmbulanceBookingStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(healthGuardService.updateAmbulanceStatus(id, status));
    }
}
