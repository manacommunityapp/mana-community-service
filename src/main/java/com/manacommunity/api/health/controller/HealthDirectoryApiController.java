package com.manacommunity.api.health.controller;

import com.manacommunity.api.health.dto.HealthDtos.*;
import com.manacommunity.api.health.entity.HealthCampRegistration;
import com.manacommunity.api.health.enums.HealthEnums.ProviderCategory;
import com.manacommunity.api.health.service.HealthDomainService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/health")
public class HealthDirectoryApiController {

    private final HealthDomainService healthDomainService;

    public HealthDirectoryApiController(HealthDomainService healthDomainService) {
        this.healthDomainService = healthDomainService;
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderSummaryDto>> getProviders(
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestParam(required = false) ProviderCategory category) {
        return ResponseEntity.ok(healthDomainService.getProviders(tenantId, category));
    }

    @GetMapping("/camps/upcoming")
    public ResponseEntity<List<CampDto>> getUpcomingCamps(
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        return ResponseEntity.ok(healthDomainService.getUpcomingCamps(tenantId));
    }

    @PostMapping("/camps/{campId}/register")
    public ResponseEntity<HealthCampRegistration> registerForCamp(
            @PathVariable Long campId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) Map<String, String> body) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        UUID patientId = (body != null && body.containsKey("patientId")) ? UUID.fromString(body.get("patientId")) : null;
        return ResponseEntity.ok(healthDomainService.registerForCamp(campId, userId, patientId));
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentDto> bookAppointment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody BookAppointmentRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.bookAppointment(tenantId, userId, req));
    }

    @GetMapping("/appointments/my")
    public ResponseEntity<List<AppointmentDto>> getMyAppointments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyAppointments(tenantId, userId));
    }

    @PutMapping("/appointments/{uuid}/cancel")
    public ResponseEntity<Map<String, String>> cancelAppointment(
            @PathVariable UUID uuid,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Cancelled by resident";
        healthDomainService.cancelAppointment(tenantId, uuid, reason);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Appointment cancelled successfully"));
    }

    @GetMapping("/patients/family")
    public ResponseEntity<List<PatientDto>> getFamilyMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getFamilyMembers(tenantId, userId));
    }

    @GetMapping("/records/{patientUuid}")
    public ResponseEntity<List<MedicalRecordDto>> getPatientRecords(
            @PathVariable UUID patientUuid,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        return ResponseEntity.ok(healthDomainService.getPatientRecords(tenantId, patientUuid));
    }

    @GetMapping("/benefits/summary")
    public ResponseEntity<BenefitsSummaryDto> getBenefitsSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getBenefitsSummary(tenantId, userId));
    }

    @PostMapping("/triage")
    public ResponseEntity<Map<String, Object>> triageSymptom(@RequestBody Map<String, String> body) {
        String query = body.getOrDefault("query", "").toLowerCase();
        boolean isEmergency = query.contains("chest pain") || query.contains("shortness of breath") || query.contains("heart") || query.contains("fainted");

        return ResponseEntity.ok(Map.of(
                "query", query,
                "isEmergency", isEmergency,
                "urgency", isEmergency ? "EMERGENCY_RED" : "ROUTINE",
                "recommendedSpecialty", isEmergency ? "Emergency Cardiology / Casualty" : "General Physician",
                "actionAdvice", isEmergency
                        ? "Immediate attention recommended. Please contact community ambulance or dial emergency 112 / Apollo Hospital."
                        : "Book a consultation with a registered community general practitioner.",
                "disclaimer", "This triage is for informational guidance only and is not an automated medical diagnosis."
        ));
    }
}
