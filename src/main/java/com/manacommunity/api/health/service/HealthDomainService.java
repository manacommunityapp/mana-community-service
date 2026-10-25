package com.manacommunity.api.health.service;

import com.manacommunity.api.health.dto.HealthDtos.*;
import com.manacommunity.api.health.entity.*;
import com.manacommunity.api.health.enums.HealthEnums.*;
import com.manacommunity.api.health.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final HealthCareJourneyRepository careJourneyRepository;
    private final HealthLabHomeCollectionRepository labHomeCollectionRepository;
    private final HealthMedicineOrderRepository medicineOrderRepository;
    private final HealthAmbulanceBookingRepository ambulanceBookingRepository;
    private final HealthEmergencyCallbackRepository emergencyCallbackRepository;
    private final HealthIpdAdmissionRepository ipdAdmissionRepository;

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
            HealthBenefitUsageRepository benefitUsageRepository,
            HealthCareJourneyRepository careJourneyRepository,
            HealthLabHomeCollectionRepository labHomeCollectionRepository,
            HealthMedicineOrderRepository medicineOrderRepository,
            HealthAmbulanceBookingRepository ambulanceBookingRepository,
            HealthEmergencyCallbackRepository emergencyCallbackRepository,
            HealthIpdAdmissionRepository ipdAdmissionRepository) {
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
        this.careJourneyRepository = careJourneyRepository;
        this.labHomeCollectionRepository = labHomeCollectionRepository;
        this.medicineOrderRepository = medicineOrderRepository;
        this.ambulanceBookingRepository = ambulanceBookingRepository;
        this.emergencyCallbackRepository = emergencyCallbackRepository;
        this.ipdAdmissionRepository = ipdAdmissionRepository;
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
            if (c.getAddressLine1() != null && !c.getAddressLine1().isBlank()) {
                dto.setMapsUrl("https://maps.google.com/?q=" + c.getAddressLine1().replace(" ", "+"));
            }
        }

        List<String> accreds = new ArrayList<>();
        if (p.getCategory() == ProviderCategory.DIAGNOSTIC_CENTER) {
            accreds.add("NABL");
            accreds.add("ISO 15189");
        } else if (p.getCategory() == ProviderCategory.CLINIC || p.getCategory() == ProviderCategory.DOCTOR || p.getCategory() == ProviderCategory.PHYSIOTHERAPIST || p.getCategory() == ProviderCategory.FITNESS_TRAINER) {
            if (Boolean.TRUE.equals(p.getIsVerified())) accreds.add("Community Verified");
        } else if (p.getCategory() == ProviderCategory.PHARMACY) {
            accreds.add("Licensed Pharmacy");
        }
        dto.setAccreditations(accreds.isEmpty() ? null : accreds);

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
    public List<CareJourneyDto> getMyCareJourneys(Long tenantId, Long residentUserId) {
        return careJourneyRepository
                .findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(tenantId, residentUserId)
                .stream().map(this::mapToCareJourneyDto).collect(Collectors.toList());
    }

    public CareJourneyDto advanceCareJourneyStage(Long tenantId, UUID careJourneyUuid, CareJourneyStage newStage) {
        HealthCareJourney journey = careJourneyRepository.findByCareJourneyUuid(careJourneyUuid)
                .orElseThrow(() -> new IllegalArgumentException("Care journey not found"));
        journey.setCurrentStage(newStage);
        LocalDateTime now = LocalDateTime.now();
        switch (newStage) {
            case CONSULTATION_COMPLETED -> journey.setConsultationCompletedAt(now);
            case PRESCRIPTION_ISSUED -> journey.setPrescriptionIssuedAt(now);
            case LAB_TEST_ORDERED -> journey.setLabTestOrderedAt(now);
            case RESULTS_READY -> journey.setResultsReadyAt(now);
            case FOLLOW_UP_SCHEDULED -> journey.setFollowUpScheduledAt(now);
            default -> { }
        }
        return mapToCareJourneyDto(careJourneyRepository.save(journey));
    }

    private CareJourneyDto mapToCareJourneyDto(HealthCareJourney j) {
        List<CareJourneyStage> allStages = Arrays.asList(CareJourneyStage.values());
        int currentOrdinal = j.getCurrentStage().ordinal();
        List<CareJourneyStepDto> steps = allStages.stream().map(stage -> {
            boolean completed = stage.ordinal() <= currentOrdinal;
            LocalDateTime completedAt = switch (stage) {
                case APPOINTMENT_BOOKED -> j.getAppointmentBookedAt();
                case CONSULTATION_COMPLETED -> j.getConsultationCompletedAt();
                case PRESCRIPTION_ISSUED -> j.getPrescriptionIssuedAt();
                case LAB_TEST_ORDERED -> j.getLabTestOrderedAt();
                case RESULTS_READY -> j.getResultsReadyAt();
                case FOLLOW_UP_SCHEDULED -> j.getFollowUpScheduledAt();
            };
            return CareJourneyStepDto.builder()
                    .stage(stage).label(stage.name().replace("_", " "))
                    .completed(completed).completedAt(completedAt).build();
        }).collect(Collectors.toList());
        return CareJourneyDto.builder()
                .id(j.getId()).careJourneyUuid(j.getCareJourneyUuid())
                .patientId(j.getPatientId()).appointmentUuid(j.getAppointmentUuid())
                .currentStage(j.getCurrentStage()).steps(steps)
                .startedAt(j.getAppointmentBookedAt()).updatedAt(j.getUpdatedAt()).build();
    }

    public LabHomeCollectionDto bookLabHomeCollection(Long tenantId, Long residentUserId,
                                                       LabHomeCollectionBookingRequest req) {
        HealthLabHomeCollection booking = HealthLabHomeCollection.builder()
                .tenantId(tenantId).residentUserId(residentUserId)
                .patientId(req.getPatientId()).packageId(req.getPackageId())
                .packageName(req.getPackageId()).collectionDate(req.getCollectionDate())
                .collectionSlot(req.getCollectionSlot()).address(req.getAddress())
                .specialInstructions(req.getSpecialInstructions())
                .phlebotomistName("Ravi Kumar (Community Lab)").build();
        return mapToLabCollectionDto(labHomeCollectionRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<LabHomeCollectionDto> getMyLabCollections(Long tenantId, Long residentUserId) {
        return labHomeCollectionRepository
                .findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(tenantId, residentUserId)
                .stream().map(this::mapToLabCollectionDto).collect(Collectors.toList());
    }

    private LabHomeCollectionDto mapToLabCollectionDto(HealthLabHomeCollection b) {
        return LabHomeCollectionDto.builder()
                .id(b.getId()).bookingUuid(b.getBookingUuid()).patientId(b.getPatientId())
                .packageName(b.getPackageName()).collectionDate(b.getCollectionDate())
                .collectionSlot(b.getCollectionSlot()).phlebotomistName(b.getPhlebotomistName())
                .status(b.getStatus()).reportUrl(b.getReportUrl()).bookedAt(b.getCreatedAt()).build();
    }

    public MedicineOrderDto placeMedicineOrder(Long tenantId, Long residentUserId,
                                                PlaceMedicineOrderRequest req) {
        HealthMedicineOrder order = HealthMedicineOrder.builder()
                .tenantId(tenantId).residentUserId(residentUserId)
                .patientId(req.getPatientId()).deliverySlot(req.getDeliverySlot())
                .deliveryAddress(req.getDeliveryAddress()).build();
        List<HealthMedicineOrderItem> items = req.getItems().stream().map(i ->
                HealthMedicineOrderItem.builder().order(order)
                        .medicineId(i.getMedicineId()).medicineName(i.getMedicineName())
                        .quantity(i.getQuantity()).unitPrice(i.getUnitPrice()).build()
        ).collect(Collectors.toList());
        order.setItems(items);
        order.setTotal(items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return mapToMedicineOrderDto(medicineOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<MedicineOrderDto> getMyMedicineOrders(Long tenantId, Long residentUserId) {
        return medicineOrderRepository
                .findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(tenantId, residentUserId)
                .stream().map(this::mapToMedicineOrderDto).collect(Collectors.toList());
    }

    private MedicineOrderDto mapToMedicineOrderDto(HealthMedicineOrder o) {
        List<MedicineOrderItemDto> items = o.getItems() == null ? List.of() :
                o.getItems().stream().map(i -> MedicineOrderItemDto.builder()
                        .medicineId(i.getMedicineId()).medicineName(i.getMedicineName())
                        .quantity(i.getQuantity()).unitPrice(i.getUnitPrice()).build())
                        .collect(Collectors.toList());
        return MedicineOrderDto.builder()
                .id(o.getId()).orderUuid(o.getOrderUuid()).patientId(o.getPatientId())
                .items(items).total(o.getTotal()).deliverySlot(o.getDeliverySlot())
                .status(o.getStatus()).placedAt(o.getCreatedAt()).build();
    }

    public AmbulanceBookingDto bookAmbulance(Long tenantId, Long residentUserId, AmbulanceBookingRequest req) {
        HealthAmbulanceBooking booking = HealthAmbulanceBooking.builder()
                .tenantId(tenantId).residentUserId(residentUserId)
                .patientId(req.getPatientId()).patientName(req.getPatientName())
                .vehicleType(req.getVehicleType() != null ? req.getVehicleType() : AmbulanceVehicleType.BASIC)
                .pickupAddress(req.getPickupAddress()).destinationAddress(req.getDestinationAddress())
                .emergencyNote(req.getEmergencyNote()).contactPhone(req.getContactPhone())
                .status(AmbulanceStatus.REQUESTED)
                .driverName("Suresh Kumar").driverPhone("+91-9800012345")
                .vehicleNumber("WB-01-AB-1234").estimatedMinutes(12)
                .build();
        return mapToAmbulanceDto(ambulanceBookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<AmbulanceBookingDto> getMyAmbulanceBookings(Long tenantId, Long residentUserId) {
        return ambulanceBookingRepository
                .findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(tenantId, residentUserId)
                .stream().map(this::mapToAmbulanceDto).collect(Collectors.toList());
    }

    private AmbulanceBookingDto mapToAmbulanceDto(HealthAmbulanceBooking b) {
        return AmbulanceBookingDto.builder()
                .id(b.getId()).bookingUuid(b.getBookingUuid()).patientId(b.getPatientId())
                .vehicleType(b.getVehicleType()).status(b.getStatus())
                .pickupAddress(b.getPickupAddress()).destinationAddress(b.getDestinationAddress())
                .driverName(b.getDriverName()).driverPhone(b.getDriverPhone())
                .vehicleNumber(b.getVehicleNumber()).estimatedMinutes(b.getEstimatedMinutes())
                .requestedAt(b.getCreatedAt()).dispatchedAt(b.getDispatchedAt())
                .arrivedAt(b.getArrivedAt()).build();
    }

    public EmergencyCallbackDto requestEmergencyCallback(Long tenantId, Long residentUserId,
                                                          EmergencyCallbackRequest req) {
        HealthEmergencyCallback callback = HealthEmergencyCallback.builder()
                .tenantId(tenantId).residentUserId(residentUserId)
                .callerName(req.getCallerName()).callerPhone(req.getCallerPhone())
                .emergencyDescription(req.getEmergencyDescription()).address(req.getAddress())
                .status(EmergencyCallbackStatus.REQUESTED)
                .assignedAgentName("Community Health Desk").build();
        return mapToCallbackDto(emergencyCallbackRepository.save(callback));
    }

    @Transactional(readOnly = true)
    public List<EmergencyCallbackDto> getMyCallbacks(Long tenantId, Long residentUserId) {
        return emergencyCallbackRepository
                .findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(tenantId, residentUserId)
                .stream().map(this::mapToCallbackDto).collect(Collectors.toList());
    }

    private EmergencyCallbackDto mapToCallbackDto(HealthEmergencyCallback c) {
        return EmergencyCallbackDto.builder()
                .id(c.getId()).callbackUuid(c.getCallbackUuid())
                .callerName(c.getCallerName()).callerPhone(c.getCallerPhone())
                .emergencyDescription(c.getEmergencyDescription())
                .status(c.getStatus()).assignedAgentName(c.getAssignedAgentName())
                .requestedAt(c.getCreatedAt()).resolvedAt(c.getResolvedAt()).build();
    }

    public IpdAdmissionDto bookIpdAdmission(Long tenantId, Long residentUserId, IpdAdmissionRequest req) {
        HealthIpdAdmission admission = HealthIpdAdmission.builder()
                .tenantId(tenantId).residentUserId(residentUserId)
                .patientId(req.getPatientId()).patientName(req.getPatientName())
                .hospitalName(req.getHospitalName()).department(req.getDepartment())
                .admissionReason(req.getAdmissionReason())
                .wardType(req.getWardPreference() != null ? req.getWardPreference() : WardType.GENERAL)
                .expectedAdmissionDate(req.getExpectedAdmissionDate())
                .attendingDoctorName(req.getAttendingDoctorName())
                .contactPhone(req.getContactPhone())
                .status(IpdAdmissionStatus.REQUESTED).build();
        return mapToIpdDto(ipdAdmissionRepository.save(admission));
    }

    @Transactional(readOnly = true)
    public List<IpdAdmissionDto> getMyIpdAdmissions(Long tenantId, Long residentUserId) {
        return ipdAdmissionRepository
                .findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(tenantId, residentUserId)
                .stream().map(this::mapToIpdDto).collect(Collectors.toList());
    }

    private IpdAdmissionDto mapToIpdDto(HealthIpdAdmission a) {
        return IpdAdmissionDto.builder()
                .id(a.getId()).admissionUuid(a.getAdmissionUuid()).patientId(a.getPatientId())
                .patientName(a.getPatientName()).hospitalName(a.getHospitalName())
                .department(a.getDepartment()).admissionReason(a.getAdmissionReason())
                .wardType(a.getWardType()).status(a.getStatus())
                .expectedAdmissionDate(a.getExpectedAdmissionDate())
                .actualAdmissionDate(a.getActualAdmissionDate())
                .dischargeDate(a.getDischargeDate()).bedNumber(a.getBedNumber())
                .attendingDoctorName(a.getAttendingDoctorName()).requestedAt(a.getCreatedAt()).build();
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

    // ── Auto-Refill Subscriptions ─────────────────────────────────────────────

    public List<PharmacyRefillSubscriptionDto> getMyRefillSubscriptions(Long tenantId, Long userId) {
        return List.of(
            PharmacyRefillSubscriptionDto.builder().id(1L).subscriptionUuid(UUID.randomUUID())
                .medicineName("Metformin 500mg").strip("30 tabs • 1 month supply").pricePerDispatch(105)
                .frequency(RefillFrequency.MONTHLY).nextDispatchDate(LocalDate.of(2026, 11, 15))
                .active(true).createdAt(LocalDateTime.now().minusDays(30)).build(),
            PharmacyRefillSubscriptionDto.builder().id(2L).subscriptionUuid(UUID.randomUUID())
                .medicineName("Amlodipine 5mg").strip("30 tabs • 1 month supply").pricePerDispatch(144)
                .frequency(RefillFrequency.MONTHLY).nextDispatchDate(LocalDate.of(2026, 11, 15))
                .active(true).createdAt(LocalDateTime.now().minusDays(30)).build()
        );
    }

    public PharmacyRefillSubscriptionDto createRefillSubscription(Long tenantId, Long userId,
            CreateRefillSubscriptionRequest req) {
        return PharmacyRefillSubscriptionDto.builder()
                .id(System.currentTimeMillis())
                .subscriptionUuid(UUID.randomUUID())
                .medicineName(req.getMedicineName())
                .strip(req.getStrip())
                .pricePerDispatch(req.getPricePerDispatch())
                .frequency(req.getFrequency())
                .nextDispatchDate(LocalDate.now().plusMonths(1))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public PharmacyRefillSubscriptionDto toggleRefillSubscription(Long tenantId, Long userId, Long id) {
        return PharmacyRefillSubscriptionDto.builder()
                .id(id).subscriptionUuid(UUID.randomUUID())
                .medicineName("Subscription #" + id).strip("30 tabs").pricePerDispatch(100)
                .frequency(RefillFrequency.MONTHLY).nextDispatchDate(LocalDate.now().plusMonths(1))
                .active(false).createdAt(LocalDateTime.now()).build();
    }

    // ── Appointment Reminders ─────────────────────────────────────────────────

    public AppointmentReminderDto setAppointmentReminder(Long userId, UUID appointmentUuid,
            SetAppointmentReminderRequest req) {
        LocalDateTime scheduledAt = switch (req.getOffset()) {
            case MIN_15  -> LocalDateTime.now().plusMinutes(15);
            case MIN_30  -> LocalDateTime.now().plusMinutes(30);
            case HOUR_1  -> LocalDateTime.now().plusHours(1);
            case HOUR_2  -> LocalDateTime.now().plusHours(2);
            case DAY_1   -> LocalDateTime.now().plusDays(1);
        };
        return AppointmentReminderDto.builder()
                .id(System.currentTimeMillis())
                .appointmentUuid(appointmentUuid)
                .offset(req.getOffset())
                .scheduledAt(scheduledAt)
                .build();
    }

    public void clearAppointmentReminder(Long userId, UUID appointmentUuid) {
        // no-op for seed; production would delete the scheduled notification
    }

    // ── IPD Bed Availability ──────────────────────────────────────────────────

    public BedAvailabilityDto getBedAvailability(String hospital) {
        Map<WardType, Integer> beds = switch (hospital) {
            case "Apollo Hospital"            -> Map.of(WardType.GENERAL, 14, WardType.SEMI_PRIVATE, 6,  WardType.PRIVATE, 4, WardType.ICU, 3);
            case "Fortis Healthcare"          -> Map.of(WardType.GENERAL, 8,  WardType.SEMI_PRIVATE, 4,  WardType.PRIVATE, 2, WardType.ICU, 1);
            case "Max Super Specialty"        -> Map.of(WardType.GENERAL, 12, WardType.SEMI_PRIVATE, 5,  WardType.PRIVATE, 3, WardType.ICU, 2);
            case "AIIMS"                      -> Map.of(WardType.GENERAL, 20, WardType.SEMI_PRIVATE, 0,  WardType.PRIVATE, 0, WardType.ICU, 4);
            case "Manipal Hospital"           -> Map.of(WardType.GENERAL, 10, WardType.SEMI_PRIVATE, 4,  WardType.PRIVATE, 3, WardType.ICU, 2);
            case "Narayana Health"            -> Map.of(WardType.GENERAL, 16, WardType.SEMI_PRIVATE, 7,  WardType.PRIVATE, 5, WardType.ICU, 3);
            default                           -> Map.of(WardType.GENERAL, 6,  WardType.SEMI_PRIVATE, 2,  WardType.PRIVATE, 1, WardType.ICU, 0);
        };
        return BedAvailabilityDto.builder()
                .hospitalName(hospital)
                .wardAvailability(beds)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    // ── Surgery Care ──────────────────────────────────────────────────────────

    public SurgeryRequestDto createSurgeryRequest(Long tenantId, Long userId, CreateSurgeryRequestRequest req) {
        return SurgeryRequestDto.builder()
                .id(1L)
                .requestUuid(UUID.randomUUID())
                .patientId(req.getPatientId())
                .procedureName(req.getProcedureName())
                .category(req.getCategory())
                .preferredHospital(req.getPreferredHospital())
                .status(SurgeryStatus.BUDDY_ASSIGNED)
                .careBuddyName("Priya Sharma")
                .careBuddyPhone("+91 98765 43210")
                .estimatedCost(req.getEstimatedCost())
                .emiTenure(req.getEmiTenure())
                .requestedDate(LocalDate.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public List<SurgeryRequestDto> getMySurgeryRequests(Long tenantId, Long userId) {
        return List.of(
                SurgeryRequestDto.builder()
                        .id(1L).requestUuid(UUID.randomUUID())
                        .procedureName("Knee Replacement (TKR)").category("Orthopedics")
                        .preferredHospital("Apollo Hospital").status(SurgeryStatus.BUDDY_ASSIGNED)
                        .careBuddyName("Priya Sharma").careBuddyPhone("+91 98765 43210")
                        .estimatedCost(200000).requestedDate(LocalDate.now().minusDays(2))
                        .updatedAt(LocalDateTime.now().minusDays(2)).build()
        );
    }

    // ── Dental ────────────────────────────────────────────────────────────────

    public List<DentalFindingDto> getDentalFindings(Long tenantId, UUID patientId) {
        return List.of(
                DentalFindingDto.builder().id(1L).patientId(patientId)
                        .dentistName("Dr. Ramesh Gupta").visitDate(LocalDate.of(2026, 8, 12))
                        .finding("Mild tartar buildup on lower incisors").treatment("Scaling done")
                        .nextVisitIn("6 months").build(),
                DentalFindingDto.builder().id(2L).patientId(patientId)
                        .dentistName("Dr. Sunita Agarwal").visitDate(LocalDate.of(2026, 3, 3))
                        .finding("Slight crowding (lower arch)").treatment("Braces consultation advised")
                        .nextVisitIn("3 months").build()
        );
    }

    public DentalPackageBookingDto bookDentalPackage(Long tenantId, Long userId, Map<String, String> body) {
        return DentalPackageBookingDto.builder()
                .id(1L).bookingUuid(UUID.randomUUID())
                .packageType(DentalPackageType.SCALING_POLISHING)
                .dentistName("Dr. Ramesh Gupta")
                .scheduledDate(LocalDate.now().plusDays(3))
                .price(799).status("CONFIRMED").build();
    }

    // ── ABHA Health ID ────────────────────────────────────────────────────────

    public AbhaProfileDto getAbhaProfile(Long tenantId, Long userId) {
        return AbhaProfileDto.builder()
                .abhaNumber("23-4521-6789-0012").name("Raghunath Krishnamurthy")
                .dateOfBirth(LocalDate.of(1985, 8, 15)).gender("Male")
                .linkedHospitals(4).linkedRecords(12).verified(true)
                .createdAt(LocalDateTime.of(2025, 3, 1, 10, 0)).build();
    }

    public AbhaProfileDto linkAbhaId(Long tenantId, Long userId, String abhaNumber) {
        return AbhaProfileDto.builder()
                .abhaNumber(abhaNumber).name("Raghunath Krishnamurthy")
                .dateOfBirth(LocalDate.of(1985, 8, 15)).gender("Male")
                .linkedHospitals(0).linkedRecords(0).verified(true)
                .createdAt(LocalDateTime.now()).build();
    }

    public List<AbhaConsentRequestDto> getAbhaConsentRequests(Long tenantId, Long userId) {
        return List.of(
                AbhaConsentRequestDto.builder().id(1L).consentUuid(UUID.randomUUID())
                        .requesterName("Apollo Hospital").purpose("View OPD Records")
                        .status(AbhaConsentStatus.PENDING).requestedDate(LocalDate.now().minusDays(1))
                        .expiryDate(LocalDate.now().plusMonths(1)).build(),
                AbhaConsentRequestDto.builder().id(2L).consentUuid(UUID.randomUUID())
                        .requesterName("Star Lab Diagnostics").purpose("Share Lab Reports")
                        .status(AbhaConsentStatus.APPROVED).requestedDate(LocalDate.now().minusDays(7))
                        .expiryDate(LocalDate.now().plusMonths(3)).build()
        );
    }

    public AbhaConsentRequestDto updateAbhaConsent(Long userId, UUID consentUuid, boolean approve) {
        return AbhaConsentRequestDto.builder()
                .id(1L).consentUuid(consentUuid)
                .requesterName("Apollo Hospital").purpose("View OPD Records")
                .status(approve ? AbhaConsentStatus.APPROVED : AbhaConsentStatus.REVOKED)
                .requestedDate(LocalDate.now()).expiryDate(LocalDate.now().plusMonths(1)).build();
    }

    // ── Second Opinion ────────────────────────────────────────────────────────

    public SecondOpinionRequestDto requestSecondOpinion(Long tenantId, Long userId, CreateSecondOpinionRequest req) {
        return SecondOpinionRequestDto.builder()
                .id(1L).requestUuid(UUID.randomUUID())
                .patientId(req.getPatientId())
                .specialistDoctorId(req.getSpecialistDoctorId())
                .specialistName("Dr. Vikram Desai")
                .cancerType(req.getCancerType())
                .originalDiagnosis(req.getOriginalDiagnosis())
                .status(SecondOpinionStatus.REQUESTED)
                .requestedAt(LocalDateTime.now()).build();
    }

    public List<SecondOpinionRequestDto> getMySecondOpinionRequests(Long tenantId, Long userId) {
        return List.of(
                SecondOpinionRequestDto.builder()
                        .id(1L).requestUuid(UUID.randomUUID())
                        .specialistName("Dr. Vikram Desai").cancerType("Breast Cancer")
                        .originalDiagnosis("Stage II — localised").status(SecondOpinionStatus.COMPLETED)
                        .opinionSummary("Staging confirmed. Recommend 4 cycles chemo followed by radiation.")
                        .requestedAt(LocalDateTime.now().minusDays(4)).completedAt(LocalDateTime.now().minusDays(2)).build()
        );
    }

    // ── Digital Prescription ──────────────────────────────────────────────────

    public DigitalPrescriptionDto generateDigitalPrescription(Long tenantId, Long userId, GeneratePrescriptionRequest req) {
        String rxId = "RX-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return DigitalPrescriptionDto.builder()
                .id(1L).prescriptionUuid(UUID.randomUUID())
                .rxId(rxId).appointmentUuid(req.getAppointmentUuid())
                .doctorName("Dr. Ramesh Mehta").doctorSpecialty("General Physician")
                .patientName("Raghunath Krishnamurthy")
                .clinicalNotes(req.getClinicalNotes())
                .medications(req.getMedications() != null ? req.getMedications() : List.of())
                .issuedDate(LocalDate.now())
                .status(DigitalPrescriptionStatus.GENERATED)
                .generatedAt(LocalDateTime.now()).build();
    }

    public List<DigitalPrescriptionDto> getMyPrescriptions(Long tenantId, Long userId) {
        return List.of(
                DigitalPrescriptionDto.builder()
                        .id(1L).rxId("RX-A3B9F1").doctorName("Dr. Ramesh Mehta")
                        .doctorSpecialty("General Physician").patientName("Raghunath Krishnamurthy")
                        .medications(List.of("Amoxicillin 500mg BD x 5 days", "Paracetamol 650mg SOS"))
                        .issuedDate(LocalDate.now().minusDays(3)).status(DigitalPrescriptionStatus.GENERATED)
                        .generatedAt(LocalDateTime.now().minusDays(3)).build()
        );
    }

    // ── Oncology Care Journey ─────────────────────────────────────────────────

    public List<OncologyJourneyDto> getMyOncologyJourneys(Long tenantId, Long userId) {
        return List.of(
                OncologyJourneyDto.builder()
                        .id(1L).journeyUuid(UUID.randomUUID())
                        .cancerType("Breast Cancer").currentStage("TREATMENT")
                        .steps(List.of(
                                OncologyJourneyStepDto.builder().stage(OncologyStage.DIAGNOSIS).label("Diagnosis Confirmed").completed(true).detail("Biopsy confirmed").completedAt(LocalDateTime.of(2026, 9, 5, 0, 0)).build(),
                                OncologyJourneyStepDto.builder().stage(OncologyStage.STAGING).label("Cancer Staging").completed(true).detail("Stage II — localised").completedAt(LocalDateTime.of(2026, 9, 12, 0, 0)).build(),
                                OncologyJourneyStepDto.builder().stage(OncologyStage.SECOND_OPINION).label("Second Opinion").completed(true).detail("Confirmed by Dr. Vikram Desai").completedAt(LocalDateTime.of(2026, 9, 18, 0, 0)).build(),
                                OncologyJourneyStepDto.builder().stage(OncologyStage.TREATMENT_PLAN).label("Treatment Plan Ready").completed(true).detail("4 cycles chemo + radiation").completedAt(LocalDateTime.of(2026, 9, 22, 0, 0)).build(),
                                OncologyJourneyStepDto.builder().stage(OncologyStage.TREATMENT).label("Treatment In Progress").completed(false).detail("Cycle 2 of 4").build(),
                                OncologyJourneyStepDto.builder().stage(OncologyStage.RESPONSE_EVAL).label("Response Evaluation").completed(false).detail("Mid-treatment CT scan").build(),
                                OncologyJourneyStepDto.builder().stage(OncologyStage.FOLLOWUP).label("Follow-up & Remission").completed(false).detail("Ongoing surveillance").build()
                        ))
                        .chemoSchedule(List.of(
                                ChemoSessionDto.builder().cycleNumber(1).scheduledDate(LocalDate.of(2026, 10, 6)).drug("Paclitaxel + Carboplatin").status("Completed").build(),
                                ChemoSessionDto.builder().cycleNumber(2).scheduledDate(LocalDate.of(2026, 10, 27)).drug("Paclitaxel + Carboplatin").status("Upcoming").build(),
                                ChemoSessionDto.builder().cycleNumber(3).scheduledDate(LocalDate.of(2026, 11, 17)).drug("Paclitaxel + Carboplatin").status("Scheduled").build(),
                                ChemoSessionDto.builder().cycleNumber(4).scheduledDate(LocalDate.of(2026, 12, 8)).drug("Paclitaxel + Carboplatin").status("Scheduled").build()
                        ))
                        .startedAt(LocalDateTime.of(2026, 9, 5, 0, 0))
                        .updatedAt(LocalDateTime.now()).build()
        );
    }

    // ── Mental Health ─────────────────────────────────────────────────────────

    public MentalHealthSessionDto bookMentalHealthSession(Long tenantId, Long userId, BookMentalHealthSessionRequest req) {
        return MentalHealthSessionDto.builder()
                .id(1L).sessionUuid(UUID.randomUUID()).patientId(req.getPatientId())
                .therapistId(req.getTherapistId()).therapistName("Dr. Priya Sharma")
                .therapistSpecialty("Clinical Psychologist")
                .sessionType(req.getSessionType())
                .sessionDate(req.getSessionDate()).sessionTime(req.getSessionTime())
                .durationMinutes(50).fee(BigDecimal.valueOf(1200)).status("CONFIRMED")
                .bookedAt(LocalDateTime.now()).build();
    }

    public List<MentalHealthSessionDto> getMyMentalHealthSessions(Long tenantId, Long userId) {
        return List.of(
                MentalHealthSessionDto.builder().id(1L).sessionUuid(UUID.randomUUID())
                        .therapistName("Dr. Priya Sharma").therapistSpecialty("Clinical Psychologist")
                        .sessionType(MentalHealthSessionType.THERAPY_SESSION)
                        .sessionDate(LocalDate.of(2026, 10, 18)).durationMinutes(50)
                        .fee(BigDecimal.valueOf(1200)).status("COMPLETED")
                        .bookedAt(LocalDateTime.of(2026, 10, 15, 10, 0)).build(),
                MentalHealthSessionDto.builder().id(2L).sessionUuid(UUID.randomUUID())
                        .therapistName("Dr. Rajan Menon").therapistSpecialty("Psychiatrist")
                        .sessionType(MentalHealthSessionType.TELECONSULT_PSYCHIATRY)
                        .sessionDate(LocalDate.of(2026, 10, 29)).durationMinutes(50)
                        .fee(BigDecimal.valueOf(1800)).status("CONFIRMED")
                        .bookedAt(LocalDateTime.now()).build()
        );
    }

    public MoodCheckInDto logMood(Long tenantId, Long userId, LogMoodRequest req) {
        return MoodCheckInDto.builder()
                .id(1L).patientId(req.getPatientId())
                .moodScore(req.getMoodScore()).note(req.getNote())
                .checkInDate(LocalDate.now()).loggedAt(LocalDateTime.now()).build();
    }

    public List<MoodCheckInDto> getMoodHistory(Long tenantId, Long userId) {
        return List.of(
                MoodCheckInDto.builder().id(1L).moodScore(4).note("Feeling better after walk").checkInDate(LocalDate.now().minusDays(2)).loggedAt(LocalDateTime.now().minusDays(2)).build(),
                MoodCheckInDto.builder().id(2L).moodScore(3).note("Bit tired").checkInDate(LocalDate.now().minusDays(1)).loggedAt(LocalDateTime.now().minusDays(1)).build(),
                MoodCheckInDto.builder().id(3L).moodScore(4).note("").checkInDate(LocalDate.now()).loggedAt(LocalDateTime.now()).build()
        );
    }

    // ── Insurance Claims ──────────────────────────────────────────────────────

    public InsuranceClaimDto intimateClaim(Long tenantId, Long userId, IntimateInsuranceClaimRequest req) {
        String claimNumber = "CLM-2026-0" + (int)(Math.random() * 900 + 100);
        return InsuranceClaimDto.builder()
                .id(1L).claimUuid(UUID.randomUUID()).claimNumber(claimNumber)
                .hospitalName(req.getHospitalName()).diagnosis(req.getDiagnosis())
                .admissionDate(req.getAdmissionDate())
                .claimedAmount(req.getEstimatedAmount())
                .status(InsuranceClaimStatus.SUBMITTED)
                .tpaName(req.getTpaName()).policyNumber(req.getPolicyNumber())
                .submittedAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    public List<InsuranceClaimDto> getMyClaims(Long tenantId, Long userId) {
        return List.of(
                InsuranceClaimDto.builder().id(1L).claimUuid(UUID.randomUUID())
                        .claimNumber("CLM-2026-00147").hospitalName("Apollo Hospitals")
                        .diagnosis("Appendicitis - Appendectomy").admissionDate(LocalDate.of(2026, 9, 12))
                        .claimedAmount(BigDecimal.valueOf(85000)).approvedAmount(BigDecimal.valueOf(82000))
                        .status(InsuranceClaimStatus.SETTLED).tpaName("Medi Assist")
                        .submittedAt(LocalDateTime.of(2026, 9, 13, 9, 0))
                        .updatedAt(LocalDateTime.of(2026, 9, 28, 0, 0)).build(),
                InsuranceClaimDto.builder().id(2L).claimUuid(UUID.randomUUID())
                        .claimNumber("CLM-2026-00298").hospitalName("Manipal Hospital")
                        .diagnosis("Knee Replacement Surgery").admissionDate(LocalDate.of(2026, 10, 1))
                        .claimedAmount(BigDecimal.valueOf(230000))
                        .status(InsuranceClaimStatus.UNDER_REVIEW).tpaName("Medi Assist")
                        .submittedAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                        .updatedAt(LocalDateTime.of(2026, 10, 15, 0, 0)).build()
        );
    }

    public InsuranceClaimDto getClaimByUuid(Long userId, UUID uuid) {
        return getMyClaims(1L, userId).stream()
                .filter(c -> c.getClaimUuid().equals(uuid))
                .findFirst().orElseThrow(() -> new RuntimeException("Claim not found"));
    }

    // ── OPD Queue ─────────────────────────────────────────────────────────────

    public OpdQueueEntryDto joinOpdQueue(Long tenantId, Long userId, JoinOpdQueueRequest req) {
        int pos = (int)(Math.random() * 4) + 2;
        return OpdQueueEntryDto.builder()
                .id(1L).queueUuid(UUID.randomUUID()).patientId(req.getPatientId())
                .doctorId(req.getDoctorId()).doctorName("Dr. Priya Nair")
                .queuePosition(pos).estimatedWaitMinutes(pos * 8)
                .status(OpdQueueStatus.WAITING).joinedAt(LocalDateTime.now()).build();
    }

    public OpdQueueEntryDto getMyQueueEntry(Long tenantId, Long userId) {
        return OpdQueueEntryDto.builder()
                .id(1L).queueUuid(UUID.randomUUID())
                .doctorName("Dr. Priya Nair").queuePosition(2)
                .estimatedWaitMinutes(16).status(OpdQueueStatus.WAITING)
                .joinedAt(LocalDateTime.now().minusMinutes(8)).build();
    }

    public void leaveOpdQueue(Long tenantId, Long userId) {
        // Seed: no-op
    }

    // ── Hospital Network ──────────────────────────────────────────────────────

    public List<NetworkHospitalDto> getNetworkHospitals(Long tenantId, String city, String specialty) {
        return List.of(
                NetworkHospitalDto.builder().id(1L).hospitalUuid(UUID.randomUUID())
                        .name("Apollo Hospitals").address("Bannerghatta Road, Bengaluru")
                        .city("Bengaluru").distanceKm(1.2).rating(4.9).totalBeds(280)
                        .cashless(true).nablAccredited(true).nabhAccredited(true).emergencyAvailable(true)
                        .specialties(List.of("Cardiology", "Oncology", "Neurology", "Multi-specialty"))
                        .networkName("Apollo Network").networkType(HospitalNetworkType.CASHLESS_TPA)
                        .contactPhone("080-26941111").build(),
                NetworkHospitalDto.builder().id(2L).hospitalUuid(UUID.randomUUID())
                        .name("Manipal Hospital").address("HAL Airport Road, Bengaluru")
                        .city("Bengaluru").distanceKm(3.4).rating(4.8).totalBeds(350)
                        .cashless(true).nablAccredited(true).nabhAccredited(true).emergencyAvailable(true)
                        .specialties(List.of("Ortho", "Cardiology", "Maternity", "Multi-specialty"))
                        .networkName("Manipal Network").networkType(HospitalNetworkType.BOTH)
                        .contactPhone("080-25024444").build(),
                NetworkHospitalDto.builder().id(3L).hospitalUuid(UUID.randomUUID())
                        .name("Fortis Hospital").address("Cunningham Road, Bengaluru")
                        .city("Bengaluru").distanceKm(4.1).rating(4.7).totalBeds(190)
                        .cashless(true).nablAccredited(true).nabhAccredited(false).emergencyAvailable(true)
                        .specialties(List.of("Oncology", "Neurology", "Multi-specialty"))
                        .networkName("Fortis Network").networkType(HospitalNetworkType.CASHLESS_TPA)
                        .contactPhone("080-66214444").build()
        );
    }

    // ── Mana Gold ─────────────────────────────────────────────────────────────

    public GoldMembershipDto subscribeGold(Long tenantId, Long userId, SubscribeGoldRequest req) {
        BigDecimal price = req.getPlanTier() == GoldPlanTier.ANNUAL ? BigDecimal.valueOf(399) : BigDecimal.valueOf(499);
        return GoldMembershipDto.builder()
                .id(1L).membershipUuid(UUID.randomUUID())
                .planTier(req.getPlanTier()).status(GoldMembershipStatus.ACTIVE)
                .monthlyPrice(price)
                .startDate(LocalDate.now())
                .renewalDate(req.getPlanTier() == GoldPlanTier.ANNUAL ? LocalDate.now().plusYears(1) : LocalDate.now().plusMonths(1))
                .freeConsultsUsed(0).freeConsultsTotal(req.getPlanTier() == GoldPlanTier.ANNUAL ? -1 : 3)
                .createdAt(LocalDateTime.now()).build();
    }

    public GoldMembershipDto getMyGoldMembership(Long tenantId, Long userId) {
        return GoldMembershipDto.builder()
                .id(1L).membershipUuid(UUID.randomUUID())
                .planTier(GoldPlanTier.ANNUAL).status(GoldMembershipStatus.ACTIVE)
                .monthlyPrice(BigDecimal.valueOf(399))
                .startDate(LocalDate.of(2026, 10, 22))
                .renewalDate(LocalDate.of(2027, 10, 22))
                .freeConsultsUsed(2).freeConsultsTotal(-1)
                .createdAt(LocalDateTime.of(2026, 10, 22, 0, 0)).build();
    }

    public void cancelGoldMembership(Long tenantId, Long userId) {
        // Seed: no-op
    }
}
