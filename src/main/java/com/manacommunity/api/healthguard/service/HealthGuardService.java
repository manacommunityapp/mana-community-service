package com.manacommunity.api.healthguard.service;

import com.manacommunity.api.healthguard.dto.*;
import com.manacommunity.api.healthguard.entity.*;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.*;
import com.manacommunity.api.healthguard.repository.*;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HealthGuardService {

    private final HealthProfileRepository profileRepository;
    private final EmergencyContactRepository contactRepository;
    private final EmergencyIncidentRepository incidentRepository;
    private final EmergencyNotificationRepository notificationRepository;
    private final CommunityResponderRepository responderRepository;
    private final AmbulanceBookingRepository ambulanceRepository;
    private final WearableDeviceRepository wearableDeviceRepository;
    private final WearableHealthEventRepository wearableEventRepository;
    private final AppUserRepository appUserRepository;
    private final ExpoPushService expoPushService;

    public HealthGuardService(
            HealthProfileRepository profileRepository,
            EmergencyContactRepository contactRepository,
            EmergencyIncidentRepository incidentRepository,
            EmergencyNotificationRepository notificationRepository,
            CommunityResponderRepository responderRepository,
            AmbulanceBookingRepository ambulanceRepository,
            WearableDeviceRepository wearableDeviceRepository,
            WearableHealthEventRepository wearableEventRepository,
            AppUserRepository appUserRepository,
            ExpoPushService expoPushService) {
        this.profileRepository = profileRepository;
        this.contactRepository = contactRepository;
        this.incidentRepository = incidentRepository;
        this.notificationRepository = notificationRepository;
        this.responderRepository = responderRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.wearableDeviceRepository = wearableDeviceRepository;
        this.wearableEventRepository = wearableEventRepository;
        this.appUserRepository = appUserRepository;
        this.expoPushService = expoPushService;
    }

    // ── Health Profile ──

    public HealthProfileDto getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(this::toProfileDto)
                .orElse(HealthProfileDto.builder().userId(userId).bloodGroup(BloodGroup.UNKNOWN).build());
    }

    @Transactional
    public HealthProfileDto saveProfile(Long userId, HealthProfileDto dto) {
        HealthProfile profile = profileRepository.findByUserId(userId)
                .orElse(HealthProfile.builder().userId(userId).build());
        profile.setBloodGroup(dto.getBloodGroup() != null ? dto.getBloodGroup() : BloodGroup.UNKNOWN);
        profile.setAllergies(dto.getAllergies());
        profile.setMedicalConditions(dto.getMedicalConditions());
        profile.setEmergencyNotes(dto.getEmergencyNotes());
        profile.setHeightCm(dto.getHeightCm());
        profile.setWeightKg(dto.getWeightKg());
        profile.setDateOfBirth(dto.getDateOfBirth());
        profile.setOrganDonorConsent(dto.getOrganDonorConsent() != null ? dto.getOrganDonorConsent() : false);
        profile.setMedicalIdNumber(dto.getMedicalIdNumber());
        profile.setPhysicianName(dto.getPhysicianName());
        profile.setPhysicianPhone(dto.getPhysicianPhone());
        return toProfileDto(profileRepository.save(profile));
    }

    // ── Emergency Contacts ──

    public List<EmergencyContactDto> getContacts(Long userId) {
        return contactRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(userId)
                .stream().map(this::toContactDto).collect(Collectors.toList());
    }

    @Transactional
    public EmergencyContactDto addContact(Long userId, EmergencyContactDto dto) {
        EmergencyContact contact = EmergencyContact.builder()
                .userId(userId)
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .relationship(dto.getRelationship() != null ? dto.getRelationship() : ContactRelationship.OTHER)
                .priority(dto.getPriority() != null ? dto.getPriority() : 1)
                .notifyOnSos(dto.getNotifyOnSos() != null ? dto.getNotifyOnSos() : true)
                .notifyOnAlert(dto.getNotifyOnAlert() != null ? dto.getNotifyOnAlert() : true)
                .consentGiven(false)
                .isActive(true)
                .build();
        return toContactDto(contactRepository.save(contact));
    }

    @Transactional
    public EmergencyContactDto updateContact(Long userId, Long contactId, EmergencyContactDto dto) {
        EmergencyContact contact = contactRepository.findByIdAndUserId(contactId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        contact.setName(dto.getName());
        contact.setPhone(dto.getPhone());
        contact.setEmail(dto.getEmail());
        if (dto.getRelationship() != null) contact.setRelationship(dto.getRelationship());
        if (dto.getPriority() != null) contact.setPriority(dto.getPriority());
        if (dto.getNotifyOnSos() != null) contact.setNotifyOnSos(dto.getNotifyOnSos());
        if (dto.getNotifyOnAlert() != null) contact.setNotifyOnAlert(dto.getNotifyOnAlert());
        return toContactDto(contactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(Long userId, Long contactId) {
        contactRepository.findByIdAndUserId(contactId, userId).ifPresent(c -> {
            c.setIsActive(false);
            contactRepository.save(c);
        });
    }

    // ── SOS & Incidents ──

    @Transactional
    public EmergencyIncidentDto triggerSos(Long userId, TriggerSosRequest request) {
        // Cancel any existing active incident first
        incidentRepository.findByUserIdAndStatus(userId, IncidentStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(IncidentStatus.CANCELLED);
                    incidentRepository.save(existing);
                });

        IncidentSeverity severity = request.getSeverity() != null
                ? request.getSeverity()
                : IncidentSeverity.LEVEL_3_POSSIBLE_EMERGENCY;
        AlertTriggerType triggerType = request.getTriggerType() != null
                ? request.getTriggerType()
                : AlertTriggerType.MANUAL_SOS;

        EmergencyIncident incident = EmergencyIncident.builder()
                .userId(userId)
                .severity(severity)
                .status(IncidentStatus.ACTIVE)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationAddress(request.getLocationAddress())
                .description(request.getDescription())
                .triggerType(triggerType)
                .build();
        incident = incidentRepository.save(incident);

        // Notify emergency contacts
        List<EmergencyContact> contacts =
                contactRepository.findByUserIdAndNotifyOnSosTrueAndIsActiveTrueOrderByPriorityAsc(userId);
        int alertsSent = 0;
        for (EmergencyContact contact : contacts) {
            String message = buildSosMessage(userId, incident, contact);
            EmergencyNotification notification = EmergencyNotification.builder()
                    .incidentId(incident.getId())
                    .recipientPhone(contact.getPhone())
                    .recipientName(contact.getName())
                    .channel(NotificationChannel.IN_APP)
                    .status(NotificationStatus.SENT)
                    .message(message)
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            alertsSent++;
        }
        incident.setAlertsSent(alertsSent);
        incident = incidentRepository.save(incident);

        // Push notifications to contacts who are also app users
        try {
            String triggererName = appUserRepository.findById(userId)
                    .map(u -> u.getFullName() != null ? u.getFullName() : "A community member")
                    .orElse("A community member");
            List<Long> contactUserIds = contacts.stream()
                    .map(c -> appUserRepository.findByPhone(c.getPhone()))
                    .filter(Optional::isPresent)
                    .map(opt -> opt.get().getId())
                    .collect(Collectors.toList());
            if (!contactUserIds.isEmpty()) {
                String pushBody = incident.getDescription() != null
                        ? incident.getDescription() : "Needs immediate assistance";
                expoPushService.sendToUsers(contactUserIds,
                        "🆘 SOS from " + triggererName,
                        pushBody,
                        Map.of("type", "SOS_ALERT", "incidentId", incident.getId()),
                        "emergency");
            }
        } catch (Exception ignored) { /* push failure must not abort SOS */ }

        // Push notifications to nearby available community responders
        try {
            List<Long> responderUserIds = responderRepository
                    .findByIsActiveTrueAndAvailabilityStatus(ResponderAvailability.AVAILABLE)
                    .stream()
                    .map(r -> r.getUserId())
                    .filter(rid -> !rid.equals(userId))
                    .collect(Collectors.toList());
            if (!responderUserIds.isEmpty()) {
                String responderName = appUserRepository.findById(userId)
                        .map(u -> u.getFullName() != null ? u.getFullName() : "A community member")
                        .orElse("A community member");
                String pushBody = incident.getDescription() != null
                        ? incident.getDescription() : "Nearby resident needs immediate assistance";
                expoPushService.sendToUsers(responderUserIds,
                        "🚨 SOS Alert — " + responderName,
                        pushBody,
                        Map.of("type", "SOS_ALERT", "incidentId", incident.getId()),
                        "emergency");
            }
        } catch (Exception ignored) { /* push failure must not abort SOS */ }

        EmergencyIncidentDto dto = toIncidentDto(incident);
        dto.setNotifications(
                notificationRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId())
                        .stream().map(this::toNotificationDto).collect(Collectors.toList())
        );
        return dto;
    }

    public EmergencyIncidentDto getActiveIncident(Long userId) {
        return incidentRepository.findByUserIdAndStatus(userId, IncidentStatus.ACTIVE)
                .map(i -> {
                    EmergencyIncidentDto dto = toIncidentDto(i);
                    dto.setNotifications(
                            notificationRepository.findByIncidentIdOrderByCreatedAtAsc(i.getId())
                                    .stream().map(this::toNotificationDto).collect(Collectors.toList())
                    );
                    return dto;
                })
                .orElse(null);
    }

    public List<EmergencyIncidentDto> getIncidentHistory(Long userId) {
        return incidentRepository.findByUserIdOrderByTriggeredAtDesc(userId)
                .stream().map(this::toIncidentDto).collect(Collectors.toList());
    }

    @Transactional
    public EmergencyIncidentDto acknowledgeIncident(Long userId, Long incidentId) {
        EmergencyIncident incident = incidentRepository.findByIdAndUserId(incidentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(LocalDateTime.now());
        return toIncidentDto(incidentRepository.save(incident));
    }

    @Transactional
    public EmergencyIncidentDto resolveIncident(Long userId, Long incidentId, String notes) {
        EmergencyIncident incident = incidentRepository.findByIdAndUserId(incidentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        if (notes != null) incident.setNotes(notes);
        return toIncidentDto(incidentRepository.save(incident));
    }

    @Transactional
    public EmergencyIncidentDto cancelIncident(Long userId, Long incidentId) {
        EmergencyIncident incident = incidentRepository.findByIdAndUserId(incidentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        incident.setStatus(IncidentStatus.CANCELLED);
        incident.setResolvedAt(LocalDateTime.now());
        return toIncidentDto(incidentRepository.save(incident));
    }

    @Transactional
    public EmergencyIncidentDto escalateIncident(Long userId, Long incidentId) {
        EmergencyIncident incident = incidentRepository.findByIdAndUserId(incidentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        IncidentSeverity next = escalate(incident.getSeverity());
        incident.setSeverity(next);
        incident.setStatus(IncidentStatus.ESCALATED);
        return toIncidentDto(incidentRepository.save(incident));
    }

    // ── Admin / Responder view ──

    public List<EmergencyIncidentDto> getActiveIncidentsAdmin() {
        return incidentRepository.findAllByStatusInOrderByTriggeredAtDesc(
                List.of(IncidentStatus.ACTIVE, IncidentStatus.ACKNOWLEDGED, IncidentStatus.ESCALATED)
        ).stream().map(this::toIncidentDto).collect(Collectors.toList());
    }

    @Transactional
    public AmbulanceBookingDto updateAmbulanceStatus(Long bookingId, AmbulanceBookingStatus status) {
        AmbulanceBooking booking = ambulanceRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        booking.setStatus(status);
        switch (status) {
            case CONFIRMED -> booking.setConfirmedAt(LocalDateTime.now());
            case DISPATCHED -> booking.setDispatchedAt(LocalDateTime.now());
            case ARRIVED -> booking.setArrivedAt(LocalDateTime.now());
            case COMPLETED -> { /* completedAt not tracked in entity yet, skip */ }
            default -> {}
        }
        return toAmbulanceDto(ambulanceRepository.save(booking));
    }

    // ── Community Responders ──

    public CommunityResponderDto getMyResponderProfile(Long userId) {
        return responderRepository.findByUserId(userId)
                .map(this::toResponderDto)
                .orElse(null);
    }

    @Transactional
    public CommunityResponderDto registerAsResponder(Long userId, CommunityResponderDto dto) {
        CommunityResponder responder = responderRepository.findByUserId(userId)
                .orElse(CommunityResponder.builder().userId(userId).build());
        if (dto.getSkills() != null) responder.setSkills(dto.getSkills());
        if (dto.getCertifications() != null) responder.setCertifications(dto.getCertifications());
        if (dto.getPhoneNumber() != null) responder.setPhoneNumber(dto.getPhoneNumber());
        responder.setIsActive(true);
        responder.setAvailabilityStatus(ResponderAvailability.AVAILABLE);
        return toResponderDto(responderRepository.save(responder));
    }

    @Transactional
    public CommunityResponderDto updateResponderStatus(Long userId, ResponderAvailability status) {
        CommunityResponder responder = responderRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Not registered as responder"));
        responder.setAvailabilityStatus(status);
        if (status == ResponderAvailability.OFFLINE) responder.setIsActive(false);
        else responder.setIsActive(true);
        responder.setLastPingAt(LocalDateTime.now());
        return toResponderDto(responderRepository.save(responder));
    }

    public long countAvailableResponders() {
        return responderRepository.countByIsActiveTrueAndAvailabilityStatus(ResponderAvailability.AVAILABLE);
    }

    // ── Ambulance Booking ──

    @Transactional
    public AmbulanceBookingDto bookAmbulance(Long userId, BookAmbulanceRequest request) {
        AmbulanceBooking booking = AmbulanceBooking.builder()
                .userId(userId)
                .incidentId(request.getIncidentId())
                .pickupAddress(request.getPickupAddress())
                .destinationAddress(request.getDestinationAddress())
                .patientName(request.getPatientName())
                .patientPhone(request.getPatientPhone())
                .emergencyDescription(request.getEmergencyDescription())
                .status(AmbulanceBookingStatus.REQUESTED)
                .build();
        return toAmbulanceDto(ambulanceRepository.save(booking));
    }

    public AmbulanceBookingDto getActiveAmbulanceBooking(Long userId) {
        return ambulanceRepository.findTopByUserIdAndStatusInOrderByRequestedAtDesc(
                userId,
                List.of(AmbulanceBookingStatus.REQUESTED, AmbulanceBookingStatus.CONFIRMED, AmbulanceBookingStatus.DISPATCHED)
        ).map(this::toAmbulanceDto).orElse(null);
    }

    public List<AmbulanceBookingDto> getAmbulanceHistory(Long userId) {
        return ambulanceRepository.findByUserIdOrderByRequestedAtDesc(userId)
                .stream().map(this::toAmbulanceDto).collect(Collectors.toList());
    }

    @Transactional
    public AmbulanceBookingDto cancelAmbulanceBooking(Long userId, Long bookingId) {
        AmbulanceBooking booking = ambulanceRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        booking.setStatus(AmbulanceBookingStatus.CANCELLED);
        return toAmbulanceDto(ambulanceRepository.save(booking));
    }

    // ── Mappers ──

    private HealthProfileDto toProfileDto(HealthProfile p) {
        return HealthProfileDto.builder()
                .id(p.getId()).userId(p.getUserId()).bloodGroup(p.getBloodGroup())
                .allergies(p.getAllergies()).medicalConditions(p.getMedicalConditions())
                .emergencyNotes(p.getEmergencyNotes()).heightCm(p.getHeightCm())
                .weightKg(p.getWeightKg()).dateOfBirth(p.getDateOfBirth())
                .organDonorConsent(p.getOrganDonorConsent()).medicalIdNumber(p.getMedicalIdNumber())
                .physicianName(p.getPhysicianName()).physicianPhone(p.getPhysicianPhone())
                .build();
    }

    private EmergencyContactDto toContactDto(EmergencyContact c) {
        return EmergencyContactDto.builder()
                .id(c.getId()).userId(c.getUserId()).name(c.getName()).phone(c.getPhone())
                .email(c.getEmail()).relationship(c.getRelationship()).priority(c.getPriority())
                .notifyOnSos(c.getNotifyOnSos()).notifyOnAlert(c.getNotifyOnAlert())
                .consentGiven(c.getConsentGiven()).isActive(c.getIsActive())
                .build();
    }

    private EmergencyIncidentDto toIncidentDto(EmergencyIncident i) {
        return EmergencyIncidentDto.builder()
                .id(i.getId()).incidentUuid(i.getIncidentUuid()).userId(i.getUserId())
                .severity(i.getSeverity()).status(i.getStatus()).latitude(i.getLatitude())
                .longitude(i.getLongitude()).locationAddress(i.getLocationAddress())
                .description(i.getDescription()).triggerType(i.getTriggerType())
                .triggeredAt(i.getTriggeredAt()).acknowledgedAt(i.getAcknowledgedAt())
                .resolvedAt(i.getResolvedAt()).notes(i.getNotes()).alertsSent(i.getAlertsSent())
                .build();
    }

    private EmergencyNotificationDto toNotificationDto(EmergencyNotification n) {
        return EmergencyNotificationDto.builder()
                .id(n.getId()).incidentId(n.getIncidentId()).recipientUserId(n.getRecipientUserId())
                .recipientPhone(n.getRecipientPhone()).recipientName(n.getRecipientName())
                .channel(n.getChannel()).status(n.getStatus()).message(n.getMessage())
                .sentAt(n.getSentAt()).acknowledgedAt(n.getAcknowledgedAt())
                .build();
    }

    private String buildSosMessage(Long userId, EmergencyIncident incident, EmergencyContact contact) {
        return String.format(
                "EMERGENCY ALERT: %s needs help! Severity: %s. Location: %s. Triggered at: %s. Please call or check on them immediately.",
                "Community Member",
                incident.getSeverity().name().replace("_", " "),
                incident.getLocationAddress() != null ? incident.getLocationAddress() : "Location not shared",
                incident.getTriggeredAt().toString()
        );
    }

    private CommunityResponderDto toResponderDto(CommunityResponder r) {
        CommunityResponderDto dto = new CommunityResponderDto();
        dto.setId(r.getId());
        dto.setUserId(r.getUserId());
        dto.setIsActive(r.getIsActive());
        dto.setAvailabilityStatus(r.getAvailabilityStatus());
        dto.setSkills(r.getSkills());
        dto.setCertifications(r.getCertifications());
        dto.setPhoneNumber(r.getPhoneNumber());
        dto.setTotalResponseCount(r.getTotalResponseCount());
        dto.setRegisteredAt(r.getRegisteredAt());
        dto.setLastPingAt(r.getLastPingAt());
        return dto;
    }

    private AmbulanceBookingDto toAmbulanceDto(AmbulanceBooking b) {
        AmbulanceBookingDto dto = new AmbulanceBookingDto();
        dto.setId(b.getId());
        dto.setBookingUuid(b.getBookingUuid());
        dto.setUserId(b.getUserId());
        dto.setIncidentId(b.getIncidentId());
        dto.setStatus(b.getStatus());
        dto.setPickupAddress(b.getPickupAddress());
        dto.setDestinationAddress(b.getDestinationAddress());
        dto.setPatientName(b.getPatientName());
        dto.setPatientPhone(b.getPatientPhone());
        dto.setEmergencyDescription(b.getEmergencyDescription());
        dto.setVehicleNumber(b.getVehicleNumber());
        dto.setDriverName(b.getDriverName());
        dto.setDriverPhone(b.getDriverPhone());
        dto.setEstimatedArrivalMinutes(b.getEstimatedArrivalMinutes());
        dto.setRequestedAt(b.getRequestedAt());
        dto.setConfirmedAt(b.getConfirmedAt());
        dto.setDispatchedAt(b.getDispatchedAt());
        dto.setArrivedAt(b.getArrivedAt());
        dto.setNotes(b.getNotes());
        return dto;
    }

    private IncidentSeverity escalate(IncidentSeverity current) {
        return switch (current) {
            case LEVEL_1_NOTICE -> IncidentSeverity.LEVEL_2_ATTENTION;
            case LEVEL_2_ATTENTION -> IncidentSeverity.LEVEL_3_POSSIBLE_EMERGENCY;
            case LEVEL_3_POSSIBLE_EMERGENCY, LEVEL_4_CONFIRMED -> IncidentSeverity.LEVEL_4_CONFIRMED;
        };
    }

    // ── Wearable Devices ──

    public List<WearableDeviceDto> getWearableDevices(Long userId) {
        return wearableDeviceRepository.findByUserIdAndIsActiveTrueOrderByRegisteredAtDesc(userId)
                .stream().map(this::toWearableDeviceDto).collect(Collectors.toList());
    }

    @Transactional
    public WearableDeviceDto registerWearableDevice(Long userId, WearableDeviceDto dto) {
        WearableDevice device = wearableDeviceRepository
                .findByUserIdAndDeviceIdentifier(userId, dto.getDeviceIdentifier())
                .orElse(WearableDevice.builder()
                        .userId(userId)
                        .deviceIdentifier(dto.getDeviceIdentifier())
                        .build());
        device.setDeviceType(dto.getDeviceType() != null ? dto.getDeviceType() : WearableType.GENERIC_BLUETOOTH);
        device.setDeviceName(dto.getDeviceName());
        device.setFirmwareVersion(dto.getFirmwareVersion());
        device.setIsActive(true);
        return toWearableDeviceDto(wearableDeviceRepository.save(device));
    }

    @Transactional
    public void removeWearableDevice(Long userId, Long deviceId) {
        wearableDeviceRepository.findByIdAndUserId(deviceId, userId).ifPresent(d -> {
            d.setIsActive(false);
            wearableDeviceRepository.save(d);
        });
    }

    // ── Wearable Health Events ──

    public List<WearableHealthEventDto> getRecentWearableEvents(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return wearableEventRepository.findByUserIdAndEventAtAfterOrderByEventAtDesc(userId, since)
                .stream().map(this::toWearableEventDto).collect(Collectors.toList());
    }

    public List<WearableHealthEventDto> getRecentWearableEventsAdmin() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return wearableEventRepository.findAllByEventAtAfterOrderByEventAtDesc(since)
                .stream().map(this::toWearableEventDto).collect(Collectors.toList());
    }

    @Transactional
    public WearableHealthEventDto submitWearableEvent(Long userId, WearableHealthEventDto dto) {
        // Update device last-sync timestamp
        if (dto.getDeviceId() != null) {
            wearableDeviceRepository.findByIdAndUserId(dto.getDeviceId(), userId).ifPresent(d -> {
                d.setLastSyncAt(LocalDateTime.now());
                wearableDeviceRepository.save(d);
            });
        }

        WearableHealthEvent event = WearableHealthEvent.builder()
                .userId(userId)
                .deviceId(dto.getDeviceId())
                .eventType(dto.getEventType())
                .heartRateBpm(dto.getHeartRateBpm())
                .spo2Percent(dto.getSpo2Percent())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .rawData(dto.getRawData())
                .sosTriggered(false)
                .eventAt(dto.getEventAt() != null ? dto.getEventAt() : LocalDateTime.now())
                .build();

        // Wearable readings are observations, not confirmed emergencies. Keep manual
        // SOS independent until any automated alert policy has clinical and operational approval.
        try {
            expoPushService.sendToUsers(
                    List.of(userId),
                    wearableAlertTitle(event.getEventType()),
                    buildWearableAlertDescription(event),
                    Map.of("type", "WEARABLE_ALERT", "eventType", event.getEventType().name()),
                    "emergency");
        } catch (Exception ignored) {}

        return toWearableEventDto(wearableEventRepository.save(event));
    }

    private String buildWearableAlertDescription(WearableHealthEvent e) {
        return switch (e.getEventType()) {
            case FALL_DETECTED -> "Your wearable reported a possible fall. Check whether you need help.";
            case HIGH_HEART_RATE -> "High heart rate detected: " + e.getHeartRateBpm() + " bpm";
            case LOW_HEART_RATE -> "Low heart rate detected: " + e.getHeartRateBpm() + " bpm";
            case IRREGULAR_HEART_RATE -> "Irregular heart rate detected";
            case LOW_SPO2 -> "Your wearable reported a blood oxygen reading of " + e.getSpo2Percent() + "%";
            case INACTIVITY_ALERT -> "Extended inactivity detected by your wearable";
            case LOW_BATTERY -> "Wearable device battery is low";
            case DEVICE_DISCONNECTED -> "Wearable device disconnected";
            case MANUAL_CHECK_IN -> "Manual check-in from wearable";
        };
    }

    private String wearableAlertTitle(WearableEventType type) {
        return switch (type) {
            case HIGH_HEART_RATE, LOW_HEART_RATE, IRREGULAR_HEART_RATE -> "❤️ Heart Rate Alert";
            case LOW_SPO2 -> "💧 Low Blood Oxygen";
            case INACTIVITY_ALERT -> "⏰ Inactivity Alert";
            case LOW_BATTERY -> "🔋 Wearable Battery Low";
            case DEVICE_DISCONNECTED -> "📵 Wearable Disconnected";
            default -> "⌚ Wearable Alert";
        };
    }

    private WearableDeviceDto toWearableDeviceDto(WearableDevice d) {
        return WearableDeviceDto.builder()
                .id(d.getId()).userId(d.getUserId()).deviceType(d.getDeviceType())
                .deviceIdentifier(d.getDeviceIdentifier()).deviceName(d.getDeviceName())
                .firmwareVersion(d.getFirmwareVersion()).isActive(d.getIsActive())
                .lastSyncAt(d.getLastSyncAt()).registeredAt(d.getRegisteredAt())
                .build();
    }

    private WearableHealthEventDto toWearableEventDto(WearableHealthEvent e) {
        return WearableHealthEventDto.builder()
                .id(e.getId()).userId(e.getUserId()).deviceId(e.getDeviceId())
                .eventType(e.getEventType()).heartRateBpm(e.getHeartRateBpm())
                .spo2Percent(e.getSpo2Percent()).latitude(e.getLatitude()).longitude(e.getLongitude())
                .rawData(e.getRawData()).sosTriggered(e.getSosTriggered())
                .linkedIncidentId(e.getLinkedIncidentId()).eventAt(e.getEventAt()).createdAt(e.getCreatedAt())
                .build();
    }
}
