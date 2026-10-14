package com.manacommunity.api.health.dto;

import com.manacommunity.api.health.enums.HealthEnums.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
}
