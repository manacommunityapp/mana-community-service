package com.manacommunity.api.health.service;

import com.manacommunity.api.health.dto.HealthDtos.*;
import com.manacommunity.api.health.entity.*;
import com.manacommunity.api.health.enums.HealthEnums.*;
import com.manacommunity.api.health.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class HealthDomainService {

    private final HealthProviderRepository providerRepository;
    private final HealthDoctorRepository doctorRepository;
    private final HealthSpecialtyRepository specialtyRepository;
    private final HealthClinicRepository clinicRepository;
    private final HealthQualificationRepository qualificationRepository;
    private final HealthRegistrationRepository registrationRepository;
    private final HealthAppointmentRepository appointmentRepository;
    private final HealthPatientRepository patientRepository;
    private final HealthPatientConsentRepository consentRepository;
    private final HealthMedicalRecordRepository medicalRecordRepository;
    private final HealthCampRepository campRepository;
    private final HealthCampRegistrationRepository campRegistrationRepository;
    private final HealthBenefitUsageRepository benefitUsageRepository;

    public HealthDomainService(
            HealthProviderRepository providerRepository,
            HealthDoctorRepository doctorRepository,
            HealthSpecialtyRepository specialtyRepository,
            HealthClinicRepository clinicRepository,
            HealthQualificationRepository qualificationRepository,
            HealthRegistrationRepository registrationRepository,
            HealthAppointmentRepository appointmentRepository,
            HealthPatientRepository patientRepository,
            HealthPatientConsentRepository consentRepository,
            HealthMedicalRecordRepository medicalRecordRepository,
            HealthCampRepository campRepository,
            HealthCampRegistrationRepository campRegistrationRepository,
            HealthBenefitUsageRepository benefitUsageRepository) {
        this.providerRepository = providerRepository;
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
        this.clinicRepository = clinicRepository;
        this.qualificationRepository = qualificationRepository;
        this.registrationRepository = registrationRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.consentRepository = consentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.campRepository = campRepository;
        this.campRegistrationRepository = campRegistrationRepository;
        this.benefitUsageRepository = benefitUsageRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderSummaryDto> getProviders(Long tenantId, ProviderCategory category) {
        List<HealthProvider> providers = (category != null)
                ? providerRepository.findByTenantIdAndCategoryAndIsActiveTrue(tenantId, category)
                : providerRepository.findByTenantIdAndIsActiveTrue(tenantId);

        return providers.stream().map(this::mapToSummary).collect(Collectors.toList());
    }

    private ProviderSummaryDto mapToSummary(HealthProvider p) {
        ProviderSummaryDto dto = ProviderSummaryDto.builder()
                .id(p.getId())
                .providerUuid(p.getProviderUuid())
                .category(p.getCategory())
                .name(p.getName())
                .primaryPhone(p.getPrimaryPhone())
                .emergencyPhone(p.getEmergencyPhone())
                .email(p.getOfficialEmail())
                .bioSummary(p.getBioSummary())
                .trustScore(p.getTrustScore())
                .consultationsCount(p.getCommunityConsultationsCount())
                .isVerified(p.getIsVerified())
                .build();

        Optional<HealthDoctor> docOpt = doctorRepository.findByProviderId(p.getId());
        if (docOpt.isPresent()) {
            HealthDoctor doc = docOpt.get();
            dto.setConsultationFee(doc.getConsultationFee());
            if (doc.getPrimarySpecialty() != null) {
                dto.setSpecialty(doc.getPrimarySpecialty().getDisplayName());
            }
            List<HealthQualification> quals = qualificationRepository.findByDoctorId(doc.getId());
            dto.setQualifications(quals.stream().map(HealthQualification::getDegreeName).collect(Collectors.toList()));
            List<HealthRegistration> regs = registrationRepository.findByDoctorId(doc.getId());
            if (!regs.isEmpty()) {
                dto.setRegistrationNumber(regs.get(0).getRegistrationNumber());
                dto.setMedicalCouncil(regs.get(0).getCouncilName());
            }
        }

        List<HealthClinic> clinics = clinicRepository.findByProviderId(p.getId());
        if (!clinics.isEmpty()) {
            HealthClinic c = clinics.get(0);
            dto.setClinicName(c.getClinicName());
            dto.setClinicAddress(c.getAddressLine1());
        }

        return dto;
    }

    public AppointmentDto bookAppointment(Long tenantId, Long userId, BookAppointmentRequest req) {
        HealthAppointment appt = HealthAppointment.builder()
                .tenantId(tenantId)
                .bookedByUserId(userId)
                .doctorId(req.getDoctorId())
                .patientId(req.getPatientId() != null ? req.getPatientId() : UUID.randomUUID())
                .patientName(req.getPatientName())
                .slotDate(req.getSlotDate())
                .slotStartTime(req.getSlotStartTime())
                .slotEndTime(req.getSlotEndTime())
                .mode(req.getMode() != null ? req.getMode() : ConsultationMode.IN_PERSON)
                .consultationFee(req.getConsultationFee() != null ? req.getConsultationFee() : BigDecimal.ZERO)
                .status(AppointmentStatus.CONFIRMED)
                .symptomsSummary(req.getSymptomsSummary())
                .build();

        HealthAppointment saved = appointmentRepository.save(appt);
        return mapToAppointmentDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getMyAppointments(Long tenantId, Long userId) {
        return appointmentRepository.findByTenantIdAndBookedByUserIdOrderBySlotDateDescSlotStartTimeDesc(tenantId, userId)
                .stream()
                .map(this::mapToAppointmentDto)
                .collect(Collectors.toList());
    }

    public void cancelAppointment(Long tenantId, UUID apptUuid, String reason) {
        HealthAppointment appt = appointmentRepository.findByAppointmentUuid(apptUuid)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        appt.setStatus(AppointmentStatus.CANCELLED);
        appt.setCancellationReason(reason);
        appointmentRepository.save(appt);
    }

    private AppointmentDto mapToAppointmentDto(HealthAppointment a) {
        return AppointmentDto.builder()
                .id(a.getId())
                .appointmentUuid(a.getAppointmentUuid())
                .doctorId(a.getDoctorId())
                .patientId(a.getPatientId())
                .patientName(a.getPatientName())
                .slotDate(a.getSlotDate())
                .slotStartTime(a.getSlotStartTime())
                .slotEndTime(a.getSlotEndTime())
                .mode(a.getMode())
                .status(a.getStatus())
                .consultationFee(a.getConsultationFee())
                .symptomsSummary(a.getSymptomsSummary())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PatientDto> getFamilyMembers(Long tenantId, Long residentUserId) {
        return patientRepository.findByTenantIdAndResidentUserIdAndIsActiveTrue(tenantId, residentUserId)
                .stream()
                .map(p -> PatientDto.builder()
                        .id(p.getId())
                        .patientUuid(p.getPatientUuid())
                        .fullName(p.getFullName())
                        .relationship(p.getRelationship())
                        .dateOfBirth(p.getDateOfBirth())
                        .gender(p.getGender())
                        .bloodGroup(p.getBloodGroup())
                        .phoneNumber(p.getPhoneNumber())
                        .emergencyContactPhone(p.getEmergencyContactPhone())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordDto> getPatientRecords(Long tenantId, UUID patientUuid) {
        return medicalRecordRepository.findByTenantIdAndPatientIdOrderByRecordDateDesc(tenantId, patientUuid)
                .stream()
                .map(r -> MedicalRecordDto.builder()
                        .id(r.getId())
                        .patientId(r.getPatientId())
                        .doctorId(r.getDoctorId())
                        .recordType(r.getRecordType())
                        .title(r.getTitle())
                        .recordDate(r.getRecordDate())
                        .clinicalNotes(r.getClinicalNotes())
                        .documentFileUrl(r.getDocumentFileUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CampDto> getUpcomingCamps(Long tenantId) {
        return campRepository.findByTenantIdAndCampDateGreaterThanEqualOrderByCampDateAsc(tenantId, LocalDate.now())
                .stream()
                .map(c -> CampDto.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .category(c.getCategory())
                        .partnerProviderName(c.getPartnerProviderName())
                        .venue(c.getVenue())
                        .campDate(c.getCampDate())
                        .startTime(c.getStartTime())
                        .endTime(c.getEndTime())
                        .maxSlots(c.getMaxSlots())
                        .registeredSlots(c.getRegisteredSlots())
                        .isFree(c.getIsFree())
                        .feeAmount(c.getFeeAmount())
                        .description(c.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    public HealthCampRegistration registerForCamp(Long campId, Long userId, UUID patientId) {
        HealthCamp camp = campRepository.findById(campId)
                .orElseThrow(() -> new IllegalArgumentException("Health camp not found"));
        camp.setRegisteredSlots(camp.getRegisteredSlots() + 1);
        campRepository.save(camp);

        String token = "CAMP-" + campId + "-" + (camp.getRegisteredSlots());
        HealthCampRegistration reg = HealthCampRegistration.builder()
                .campId(campId)
                .userId(userId)
                .patientId(patientId)
                .tokenNumber(token)
                .build();
        return campRegistrationRepository.save(reg);
    }

    @Transactional(readOnly = true)
    public BenefitsSummaryDto getBenefitsSummary(Long tenantId, Long residentUserId) {
        HealthBenefitUsage usage = benefitUsageRepository.findByTenantIdAndResidentUserId(tenantId, residentUserId)
                .orElseGet(() -> HealthBenefitUsage.builder()
                        .tenantId(tenantId)
                        .residentUserId(residentUserId)
                        .communityAllowance(new BigDecimal("5000.00"))
                        .usedAllowance(BigDecimal.ZERO)
                        .cashlessActive(true)
                        .build());

        BigDecimal rem = usage.getCommunityAllowance().subtract(usage.getUsedAllowance());
        return BenefitsSummaryDto.builder()
                .communityAllowance(usage.getCommunityAllowance())
                .usedAllowance(usage.getUsedAllowance())
                .remainingAllowance(rem.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : rem)
                .cashlessActive(usage.getCashlessActive())
                .build();
    }
}
