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
        } else if (p.getCategory() == ProviderCategory.CLINIC || p.getCategory() == ProviderCategory.DOCTOR) {
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
}
