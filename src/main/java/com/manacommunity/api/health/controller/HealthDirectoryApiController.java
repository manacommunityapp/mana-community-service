package com.manacommunity.api.health.controller;

import com.manacommunity.api.health.dto.HealthDtos.*;
import com.manacommunity.api.health.entity.HealthCampRegistration;
import com.manacommunity.api.health.enums.HealthEnums.CareJourneyStage;
import com.manacommunity.api.health.enums.HealthEnums.ProviderCategory;
import com.manacommunity.api.health.service.HealthDomainService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.UUID;

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

    @GetMapping("/care-journeys/my")
    public ResponseEntity<List<CareJourneyDto>> getMyCareJourneys(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyCareJourneys(tenantId, userId));
    }

    @PutMapping("/care-journeys/{uuid}/stage")
    public ResponseEntity<CareJourneyDto> advanceCareJourneyStage(
            @PathVariable UUID uuid,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody Map<String, String> body) {
        CareJourneyStage stage = CareJourneyStage.valueOf(body.getOrDefault("stage", "APPOINTMENT_BOOKED"));
        return ResponseEntity.ok(healthDomainService.advanceCareJourneyStage(tenantId, uuid, stage));
    }

    @PostMapping("/labs/home-collection/book")
    public ResponseEntity<LabHomeCollectionDto> bookLabHomeCollection(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody LabHomeCollectionBookingRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.bookLabHomeCollection(tenantId, userId, req));
    }

    @GetMapping("/labs/home-collection/my")
    public ResponseEntity<List<LabHomeCollectionDto>> getMyLabCollections(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyLabCollections(tenantId, userId));
    }

    @PostMapping("/pharmacy/orders")
    public ResponseEntity<MedicineOrderDto> placeMedicineOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody PlaceMedicineOrderRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.placeMedicineOrder(tenantId, userId, req));
    }

    @GetMapping("/pharmacy/orders/my")
    public ResponseEntity<List<MedicineOrderDto>> getMyMedicineOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyMedicineOrders(tenantId, userId));
    }

    @PostMapping("/ambulance/book")
    public ResponseEntity<AmbulanceBookingDto> bookAmbulance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody AmbulanceBookingRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.bookAmbulance(tenantId, userId, req));
    }

    @GetMapping("/ambulance/my")
    public ResponseEntity<List<AmbulanceBookingDto>> getMyAmbulanceBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyAmbulanceBookings(tenantId, userId));
    }

    @PostMapping("/emergency/callback")
    public ResponseEntity<EmergencyCallbackDto> requestEmergencyCallback(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody EmergencyCallbackRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.requestEmergencyCallback(tenantId, userId, req));
    }

    @GetMapping("/emergency/callbacks/my")
    public ResponseEntity<List<EmergencyCallbackDto>> getMyCallbacks(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyCallbacks(tenantId, userId));
    }

    @PostMapping("/ipd/book")
    public ResponseEntity<IpdAdmissionDto> bookIpdAdmission(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody IpdAdmissionRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.bookIpdAdmission(tenantId, userId, req));
    }

    @GetMapping("/ipd/my")
    public ResponseEntity<List<IpdAdmissionDto>> getMyIpdAdmissions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyIpdAdmissions(tenantId, userId));
    }

    @GetMapping("/departments")
    public ResponseEntity<List<String>> getDepartments() {
        return ResponseEntity.ok(List.of(
                "General Medicine", "Cardiology", "Orthopedics", "Pediatrics",
                "Gynecology", "Dermatology", "ENT", "Ophthalmology",
                "Neurology", "Psychiatry", "Dental", "Physiotherapy"
        ));
    }

    // ── Auto-Refill Subscriptions ─────────────────────────────────────────────

    @GetMapping("/pharmacy/subscriptions/my")
    public ResponseEntity<List<PharmacyRefillSubscriptionDto>> getMyRefillSubscriptions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyRefillSubscriptions(tenantId, userId));
    }

    @PostMapping("/pharmacy/subscriptions")
    public ResponseEntity<PharmacyRefillSubscriptionDto> createRefillSubscription(
            @RequestBody CreateRefillSubscriptionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.createRefillSubscription(tenantId, userId, request));
    }

    @PatchMapping("/pharmacy/subscriptions/{id}/toggle")
    public ResponseEntity<PharmacyRefillSubscriptionDto> toggleRefillSubscription(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.toggleRefillSubscription(tenantId, userId, id));
    }

    // ── Appointment Reminders ─────────────────────────────────────────────────

    @PostMapping("/appointments/{uuid}/reminder")
    public ResponseEntity<AppointmentReminderDto> setAppointmentReminder(
            @PathVariable UUID uuid,
            @RequestBody SetAppointmentReminderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.setAppointmentReminder(userId, uuid, request));
    }

    @DeleteMapping("/appointments/{uuid}/reminder")
    public ResponseEntity<Void> clearAppointmentReminder(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        healthDomainService.clearAppointmentReminder(userId, uuid);
        return ResponseEntity.noContent().build();
    }

    // ── IPD Bed Availability ──────────────────────────────────────────────────

    @GetMapping("/ipd/bed-availability")
    public ResponseEntity<BedAvailabilityDto> getBedAvailability(
            @RequestParam String hospital) {
        return ResponseEntity.ok(healthDomainService.getBedAvailability(hospital));
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
