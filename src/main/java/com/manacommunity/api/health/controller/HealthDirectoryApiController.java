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

    // ── Surgery Care ──────────────────────────────────────────────────────────

    @PostMapping("/surgery/request")
    public ResponseEntity<SurgeryRequestDto> createSurgeryRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody CreateSurgeryRequestRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.createSurgeryRequest(tenantId, userId, req));
    }

    @GetMapping("/surgery/my")
    public ResponseEntity<List<SurgeryRequestDto>> getMySurgeryRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMySurgeryRequests(tenantId, userId));
    }

    // ── Dental ────────────────────────────────────────────────────────────────

    @GetMapping("/dental/findings/{patientUuid}")
    public ResponseEntity<List<DentalFindingDto>> getDentalFindings(
            @PathVariable UUID patientUuid,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        return ResponseEntity.ok(healthDomainService.getDentalFindings(tenantId, patientUuid));
    }

    @PostMapping("/dental/packages/book")
    public ResponseEntity<DentalPackageBookingDto> bookDentalPackage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody Map<String, String> body) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.bookDentalPackage(tenantId, userId, body));
    }

    // ── ABHA Health ID ────────────────────────────────────────────────────────

    @GetMapping("/abha/profile")
    public ResponseEntity<AbhaProfileDto> getAbhaProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getAbhaProfile(tenantId, userId));
    }

    @PostMapping("/abha/link")
    public ResponseEntity<AbhaProfileDto> linkAbhaId(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody Map<String, String> body) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.linkAbhaId(tenantId, userId, body.get("abhaNumber")));
    }

    @GetMapping("/abha/consent-requests")
    public ResponseEntity<List<AbhaConsentRequestDto>> getAbhaConsentRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getAbhaConsentRequests(tenantId, userId));
    }

    @PatchMapping("/abha/consent-requests/{uuid}/approve")
    public ResponseEntity<AbhaConsentRequestDto> approveAbhaConsent(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.updateAbhaConsent(userId, uuid, true));
    }

    @PatchMapping("/abha/consent-requests/{uuid}/revoke")
    public ResponseEntity<AbhaConsentRequestDto> revokeAbhaConsent(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.updateAbhaConsent(userId, uuid, false));
    }

    // ── Second Opinion ────────────────────────────────────────────────────────

    @PostMapping("/second-opinion/request")
    public ResponseEntity<SecondOpinionRequestDto> requestSecondOpinion(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody CreateSecondOpinionRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.requestSecondOpinion(tenantId, userId, req));
    }

    @GetMapping("/second-opinion/my")
    public ResponseEntity<List<SecondOpinionRequestDto>> getMySecondOpinionRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMySecondOpinionRequests(tenantId, userId));
    }

    // ── Digital Prescription ──────────────────────────────────────────────────

    @PostMapping("/prescriptions/generate")
    public ResponseEntity<DigitalPrescriptionDto> generateDigitalPrescription(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody GeneratePrescriptionRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.generateDigitalPrescription(tenantId, userId, req));
    }

    @GetMapping("/prescriptions/my")
    public ResponseEntity<List<DigitalPrescriptionDto>> getMyPrescriptions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyPrescriptions(tenantId, userId));
    }

    // ── Oncology Care Journey ─────────────────────────────────────────────────

    @GetMapping("/oncology/journey/my")
    public ResponseEntity<List<OncologyJourneyDto>> getMyOncologyJourneys(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyOncologyJourneys(tenantId, userId));
    }

    // ── Mental Health ─────────────────────────────────────────────────────────

    @PostMapping("/mental-health/sessions/book")
    public ResponseEntity<MentalHealthSessionDto> bookMentalHealthSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody BookMentalHealthSessionRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.bookMentalHealthSession(tenantId, userId, req));
    }

    @GetMapping("/mental-health/sessions/my")
    public ResponseEntity<List<MentalHealthSessionDto>> getMyMentalHealthSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyMentalHealthSessions(tenantId, userId));
    }

    @PostMapping("/mental-health/mood")
    public ResponseEntity<MoodCheckInDto> logMood(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody LogMoodRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.logMood(tenantId, userId, req));
    }

    @GetMapping("/mental-health/mood/history")
    public ResponseEntity<List<MoodCheckInDto>> getMoodHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMoodHistory(tenantId, userId));
    }

    // ── Insurance Claims ──────────────────────────────────────────────────────

    @PostMapping("/insurance/claims/intimate")
    public ResponseEntity<InsuranceClaimDto> intimateClaim(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody IntimateInsuranceClaimRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.intimateClaim(tenantId, userId, req));
    }

    @GetMapping("/insurance/claims/my")
    public ResponseEntity<List<InsuranceClaimDto>> getMyClaims(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyClaims(tenantId, userId));
    }

    @GetMapping("/insurance/claims/{uuid}")
    public ResponseEntity<InsuranceClaimDto> getClaimByUuid(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getClaimByUuid(userId, uuid));
    }

    // ── OPD Queue ─────────────────────────────────────────────────────────────

    @PostMapping("/queue/join")
    public ResponseEntity<OpdQueueEntryDto> joinOpdQueue(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody JoinOpdQueueRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.joinOpdQueue(tenantId, userId, req));
    }

    @GetMapping("/queue/my")
    public ResponseEntity<OpdQueueEntryDto> getMyQueueEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyQueueEntry(tenantId, userId));
    }

    @DeleteMapping("/queue/leave")
    public ResponseEntity<Void> leaveOpdQueue(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        healthDomainService.leaveOpdQueue(tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Hospital Network ──────────────────────────────────────────────────────

    @GetMapping("/hospitals/network")
    public ResponseEntity<List<NetworkHospitalDto>> getNetworkHospitals(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String specialty) {
        return ResponseEntity.ok(healthDomainService.getNetworkHospitals(tenantId, city, specialty));
    }

    // ── Mana Gold ─────────────────────────────────────────────────────────────

    @PostMapping("/gold/subscribe")
    public ResponseEntity<GoldMembershipDto> subscribeGold(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestBody SubscribeGoldRequest req) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.subscribeGold(tenantId, userId, req));
    }

    @GetMapping("/gold/my")
    public ResponseEntity<GoldMembershipDto> getMyGoldMembership(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getMyGoldMembership(tenantId, userId));
    }

    @DeleteMapping("/gold/cancel")
    public ResponseEntity<Void> cancelGoldMembership(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        healthDomainService.cancelGoldMembership(tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Round 6: Condition Groups, Language Preference, Insurance Coverage ────

    @GetMapping("/specialties/condition-groups")
    public ResponseEntity<List<ConditionGroupDto>> getConditionGroups(
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        return ResponseEntity.ok(healthDomainService.getConditionGroups(tenantId));
    }

    @PostMapping("/appointments/language-preference")
    public ResponseEntity<Void> setLanguagePreference(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody LanguagePreferenceRequest req,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        healthDomainService.setLanguagePreference(tenantId, userId, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/surgery/insurance-coverage/{procedureId}")
    public ResponseEntity<InsuranceCoverageDto> getSurgeryInsuranceCoverage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String procedureId,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        Long userId = (principal != null) ? principal.getId() : 1L;
        return ResponseEntity.ok(healthDomainService.getSurgeryInsuranceCoverage(tenantId, userId, procedureId));
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
