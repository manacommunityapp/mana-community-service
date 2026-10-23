package com.manacommunity.api.health.dto;

import com.manacommunity.api.health.enums.HealthEnums.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HealthDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderSummaryDto {
        private Long id;
        private UUID providerUuid;
        private ProviderCategory category;
        private String name;
        private String specialty;
        private String primaryPhone;
        private String emergencyPhone;
        private String email;
        private String bioSummary;
        private BigDecimal trustScore;
        private Integer consultationsCount;
        private Boolean isVerified;
        private String clinicName;
        private String clinicAddress;
        private BigDecimal consultationFee;
        private List<String> qualifications;
        private String registrationNumber;
        private String medicalCouncil;
        private List<String> accreditations;
        private String mapsUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookAppointmentRequest {
        private Long doctorId;
        private UUID patientId;
        private String patientName;
        private LocalDate slotDate;
        private LocalTime slotStartTime;
        private LocalTime slotEndTime;
        private ConsultationMode mode;
        private BigDecimal consultationFee;
        private String symptomsSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentDto {
        private Long id;
        private UUID appointmentUuid;
        private Long doctorId;
        private String doctorName;
        private String specialty;
        private UUID patientId;
        private String patientName;
        private LocalDate slotDate;
        private LocalTime slotStartTime;
        private LocalTime slotEndTime;
        private ConsultationMode mode;
        private AppointmentStatus status;
        private BigDecimal consultationFee;
        private String symptomsSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientDto {
        private Long id;
        private UUID patientUuid;
        private String fullName;
        private FamilyRelationship relationship;
        private LocalDate dateOfBirth;
        private String gender;
        private String bloodGroup;
        private String phoneNumber;
        private String emergencyContactPhone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicalRecordDto {
        private Long id;
        private UUID patientId;
        private Long doctorId;
        private RecordType recordType;
        private String title;
        private LocalDate recordDate;
        private String clinicalNotes;
        private String documentFileUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampDto {
        private Long id;
        private String title;
        private CampCategory category;
        private String partnerProviderName;
        private String venue;
        private LocalDate campDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer maxSlots;
        private Integer registeredSlots;
        private Boolean isFree;
        private BigDecimal feeAmount;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenefitsSummaryDto {
        private BigDecimal communityAllowance;
        private BigDecimal usedAllowance;
        private BigDecimal remainingAllowance;
        private Boolean cashlessActive;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CareJourneyStepDto {
        private CareJourneyStage stage;
        private String label;
        private boolean completed;
        private LocalDateTime completedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CareJourneyDto {
        private Long id;
        private UUID careJourneyUuid;
        private UUID patientId;
        private UUID appointmentUuid;
        private CareJourneyStage currentStage;
        private List<CareJourneyStepDto> steps;
        private LocalDateTime startedAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LabHomeCollectionBookingRequest {
        private String packageId;
        private UUID patientId;
        private String collectionDate;
        private String collectionSlot;
        private String address;
        private String specialInstructions;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LabHomeCollectionDto {
        private Long id;
        private UUID bookingUuid;
        private UUID patientId;
        private String packageName;
        private String collectionDate;
        private String collectionSlot;
        private String phlebotomistName;
        private LabCollectionStatus status;
        private String reportUrl;
        private LocalDateTime bookedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MedicineOrderItemDto {
        private String medicineId;
        private String medicineName;
        private int quantity;
        private BigDecimal unitPrice;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MedicineOrderDto {
        private Long id;
        private UUID orderUuid;
        private UUID patientId;
        private List<MedicineOrderItemDto> items;
        private BigDecimal total;
        private String deliverySlot;
        private MedicineOrderStatus status;
        private LocalDateTime placedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlaceMedicineOrderRequest {
        private UUID patientId;
        private List<MedicineOrderItemDto> items;
        private String deliverySlot;
        private String deliveryAddress;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AmbulanceBookingRequest {
        private UUID patientId;
        private String patientName;
        private AmbulanceVehicleType vehicleType;
        private String pickupAddress;
        private String destinationAddress;
        private String emergencyNote;
        private String contactPhone;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AmbulanceBookingDto {
        private Long id;
        private UUID bookingUuid;
        private UUID patientId;
        private AmbulanceVehicleType vehicleType;
        private AmbulanceStatus status;
        private String pickupAddress;
        private String destinationAddress;
        private String driverName;
        private String driverPhone;
        private String vehicleNumber;
        private Integer estimatedMinutes;
        private LocalDateTime requestedAt;
        private LocalDateTime dispatchedAt;
        private LocalDateTime arrivedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EmergencyCallbackRequest {
        private String callerName;
        private String callerPhone;
        private String emergencyDescription;
        private String address;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EmergencyCallbackDto {
        private Long id;
        private UUID callbackUuid;
        private String callerName;
        private String callerPhone;
        private String emergencyDescription;
        private EmergencyCallbackStatus status;
        private String assignedAgentName;
        private LocalDateTime requestedAt;
        private LocalDateTime resolvedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IpdAdmissionRequest {
        private UUID patientId;
        private String patientName;
        private String hospitalName;
        private String department;
        private String admissionReason;
        private WardType wardPreference;
        private LocalDate expectedAdmissionDate;
        private String attendingDoctorName;
        private String contactPhone;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IpdAdmissionDto {
        private Long id;
        private UUID admissionUuid;
        private UUID patientId;
        private String patientName;
        private String hospitalName;
        private String department;
        private String admissionReason;
        private WardType wardType;
        private IpdAdmissionStatus status;
        private LocalDate expectedAdmissionDate;
        private LocalDate actualAdmissionDate;
        private LocalDate dischargeDate;
        private String bedNumber;
        private String attendingDoctorName;
        private LocalDateTime requestedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PharmacyRefillSubscriptionDto {
        private Long id;
        private UUID subscriptionUuid;
        private String medicineName;
        private String strip;
        private Integer pricePerDispatch;
        private RefillFrequency frequency;
        private LocalDate nextDispatchDate;
        private boolean active;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateRefillSubscriptionRequest {
        private String medicineName;
        private String strip;
        private Integer pricePerDispatch;
        private RefillFrequency frequency;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AppointmentReminderDto {
        private Long id;
        private UUID appointmentUuid;
        private AppointmentReminderOffset offset;
        private LocalDateTime scheduledAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SetAppointmentReminderRequest {
        private AppointmentReminderOffset offset;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BedAvailabilityDto {
        private String hospitalName;
        private Map<WardType, Integer> wardAvailability;
        private LocalDateTime lastUpdated;
    }

    // ── Surgery Care ──────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SurgeryRequestDto {
        private Long id;
        private UUID requestUuid;
        private UUID patientId;
        private String procedureName;
        private String category;
        private String preferredHospital;
        private SurgeryStatus status;
        private String careBuddyName;
        private String careBuddyPhone;
        private Integer estimatedCost;
        private String emiTenure;
        private LocalDate requestedDate;
        private LocalDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateSurgeryRequestRequest {
        private UUID patientId;
        private String procedureName;
        private String category;
        private String preferredHospital;
        private Integer estimatedCost;
        private String emiTenure;
    }

    // ── Dental ────────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DentalFindingDto {
        private Long id;
        private UUID patientId;
        private String dentistName;
        private LocalDate visitDate;
        private String finding;
        private String treatment;
        private String nextVisitIn;
        private String documentUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DentalPackageBookingDto {
        private Long id;
        private UUID bookingUuid;
        private UUID patientId;
        private DentalPackageType packageType;
        private String dentistName;
        private LocalDate scheduledDate;
        private Integer price;
        private String status;
    }

    // ── ABHA Health ID ────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AbhaProfileDto {
        private String abhaNumber;
        private String name;
        private LocalDate dateOfBirth;
        private String gender;
        private Integer linkedHospitals;
        private Integer linkedRecords;
        private boolean verified;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AbhaConsentRequestDto {
        private Long id;
        private UUID consentUuid;
        private String requesterName;
        private String purpose;
        private AbhaConsentStatus status;
        private LocalDate requestedDate;
        private LocalDate expiryDate;
    }

    // ── Second Opinion ────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SecondOpinionRequestDto {
        private Long id;
        private UUID requestUuid;
        private UUID patientId;
        private Long specialistDoctorId;
        private String specialistName;
        private String cancerType;
        private String originalDiagnosis;
        private SecondOpinionStatus status;
        private String opinionSummary;
        private LocalDateTime requestedAt;
        private LocalDateTime completedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateSecondOpinionRequest {
        private UUID patientId;
        private Long specialistDoctorId;
        private String cancerType;
        private String originalDiagnosis;
        private List<UUID> attachedRecordIds;
    }

    // ── Digital Prescription ──────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DigitalPrescriptionDto {
        private Long id;
        private UUID prescriptionUuid;
        private String rxId;
        private UUID appointmentUuid;
        private Long doctorId;
        private String doctorName;
        private String doctorSpecialty;
        private UUID patientId;
        private String patientName;
        private String clinicalNotes;
        private List<String> medications;
        private LocalDate issuedDate;
        private DigitalPrescriptionStatus status;
        private String pdfUrl;
        private LocalDateTime generatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GeneratePrescriptionRequest {
        private UUID appointmentUuid;
        private String clinicalNotes;
        private List<String> medications;
    }

    // ── Cancer / Oncology Care Journey ────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OncologyJourneyDto {
        private Long id;
        private UUID journeyUuid;
        private UUID patientId;
        private String cancerType;
        private String currentStage;
        private List<OncologyJourneyStepDto> steps;
        private List<ChemoSessionDto> chemoSchedule;
        private LocalDateTime startedAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OncologyJourneyStepDto {
        private OncologyStage stage;
        private String label;
        private boolean completed;
        private String detail;
        private LocalDateTime completedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChemoSessionDto {
        private Integer cycleNumber;
        private LocalDate scheduledDate;
        private String drug;
        private String status;
    }

    // ── Mental Health ─────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MentalHealthSessionDto {
        private Long id;
        private UUID sessionUuid;
        private UUID patientId;
        private Long therapistId;
        private String therapistName;
        private String therapistSpecialty;
        private MentalHealthSessionType sessionType;
        private LocalDate sessionDate;
        private LocalTime sessionTime;
        private Integer durationMinutes;
        private BigDecimal fee;
        private String status;
        private LocalDateTime bookedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BookMentalHealthSessionRequest {
        private UUID patientId;
        private Long therapistId;
        private MentalHealthSessionType sessionType;
        private LocalDate sessionDate;
        private LocalTime sessionTime;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MoodCheckInDto {
        private Long id;
        private UUID patientId;
        private Integer moodScore;
        private String note;
        private LocalDate checkInDate;
        private LocalDateTime loggedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LogMoodRequest {
        private UUID patientId;
        private Integer moodScore;
        private String note;
    }

    // ── Insurance Claims ──────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InsuranceClaimDto {
        private Long id;
        private UUID claimUuid;
        private String claimNumber;
        private UUID patientId;
        private String hospitalName;
        private String diagnosis;
        private LocalDate admissionDate;
        private BigDecimal claimedAmount;
        private BigDecimal approvedAmount;
        private InsuranceClaimStatus status;
        private String tpaName;
        private String policyNumber;
        private LocalDateTime submittedAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IntimateInsuranceClaimRequest {
        private UUID patientId;
        private String hospitalName;
        private String diagnosis;
        private LocalDate admissionDate;
        private BigDecimal estimatedAmount;
        private String tpaName;
        private String policyNumber;
    }

    // ── OPD Queue ─────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OpdQueueEntryDto {
        private Long id;
        private UUID queueUuid;
        private UUID patientId;
        private Long doctorId;
        private String doctorName;
        private Integer queuePosition;
        private Integer estimatedWaitMinutes;
        private OpdQueueStatus status;
        private LocalDateTime joinedAt;
        private LocalDateTime calledAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class JoinOpdQueueRequest {
        private UUID patientId;
        private Long doctorId;
        private ConsultationMode mode;
    }

    // ── Hospital Network ──────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NetworkHospitalDto {
        private Long id;
        private UUID hospitalUuid;
        private String name;
        private String address;
        private String city;
        private Double distanceKm;
        private Double rating;
        private Integer totalBeds;
        private boolean cashless;
        private boolean nablAccredited;
        private boolean nabhAccredited;
        private boolean emergencyAvailable;
        private List<String> specialties;
        private String networkName;
        private HospitalNetworkType networkType;
        private String contactPhone;
        private String mapsUrl;
    }

    // ── Mana Gold ─────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GoldMembershipDto {
        private Long id;
        private UUID membershipUuid;
        private UUID patientId;
        private GoldPlanTier planTier;
        private GoldMembershipStatus status;
        private BigDecimal monthlyPrice;
        private LocalDate startDate;
        private LocalDate renewalDate;
        private Integer freeConsultsUsed;
        private Integer freeConsultsTotal;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubscribeGoldRequest {
        private UUID patientId;
        private GoldPlanTier planTier;
    }
}
