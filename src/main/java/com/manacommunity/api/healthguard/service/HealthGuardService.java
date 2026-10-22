package com.manacommunity.api.healthguard.service;

import com.manacommunity.api.healthguard.dto.*;
import com.manacommunity.api.healthguard.entity.*;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.*;
import com.manacommunity.api.healthguard.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HealthGuardService {

    private final HealthProfileRepository profileRepository;
    private final EmergencyContactRepository contactRepository;
    private final EmergencyIncidentRepository incidentRepository;
    private final EmergencyNotificationRepository notificationRepository;

    public HealthGuardService(
            HealthProfileRepository profileRepository,
            EmergencyContactRepository contactRepository,
            EmergencyIncidentRepository incidentRepository,
            EmergencyNotificationRepository notificationRepository) {
        this.profileRepository = profileRepository;
        this.contactRepository = contactRepository;
        this.incidentRepository = incidentRepository;
        this.notificationRepository = notificationRepository;
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

    private IncidentSeverity escalate(IncidentSeverity current) {
        return switch (current) {
            case LEVEL_1_NOTICE -> IncidentSeverity.LEVEL_2_ATTENTION;
            case LEVEL_2_ATTENTION -> IncidentSeverity.LEVEL_3_POSSIBLE_EMERGENCY;
            case LEVEL_3_POSSIBLE_EMERGENCY, LEVEL_4_CONFIRMED -> IncidentSeverity.LEVEL_4_CONFIRMED;
        };
    }
}
