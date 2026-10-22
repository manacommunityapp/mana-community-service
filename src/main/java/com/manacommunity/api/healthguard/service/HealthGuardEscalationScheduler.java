package com.manacommunity.api.healthguard.service;

import com.manacommunity.api.healthguard.entity.EmergencyContact;
import com.manacommunity.api.healthguard.entity.EmergencyIncident;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.*;
import com.manacommunity.api.healthguard.repository.EmergencyContactRepository;
import com.manacommunity.api.healthguard.repository.EmergencyIncidentRepository;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthGuardEscalationScheduler {

    private final EmergencyIncidentRepository incidentRepository;
    private final EmergencyContactRepository contactRepository;
    private final AppUserRepository appUserRepository;
    private final ExpoPushService expoPushService;

    /** Escalate ACTIVE incidents that have gone unacknowledged for 10+ minutes. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void escalateUnacknowledgedIncidents() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<EmergencyIncident> stale =
                incidentRepository.findByStatusAndTriggeredAtBefore(IncidentStatus.ACTIVE, threshold);

        for (EmergencyIncident incident : stale) {
            incident.setStatus(IncidentStatus.ESCALATED);
            incident.setSeverity(IncidentSeverity.LEVEL_4_CONFIRMED);
            incidentRepository.save(incident);

            log.warn("HealthGuard: escalated incident {} (user {}) — no acknowledgement after 10 min",
                    incident.getId(), incident.getUserId());

            notifyContactsOfEscalation(incident);
        }
    }

    private void notifyContactsOfEscalation(EmergencyIncident incident) {
        try {
            List<EmergencyContact> contacts = contactRepository
                    .findByUserIdAndNotifyOnSosTrueAndIsActiveTrueOrderByPriorityAsc(incident.getUserId());

            String triggererName = appUserRepository.findById(incident.getUserId())
                    .map(u -> u.getFirstName() != null ? u.getFirstName() : "A community member")
                    .orElse("A community member");

            List<Long> contactUserIds = contacts.stream()
                    .map(c -> appUserRepository.findByPhone(c.getPhone()))
                    .filter(Optional::isPresent)
                    .map(opt -> opt.get().getId())
                    .collect(Collectors.toList());

            if (!contactUserIds.isEmpty()) {
                expoPushService.sendToUsers(
                        contactUserIds,
                        "🚨 ESCALATED: SOS from " + triggererName,
                        "No one has responded yet — please check immediately",
                        Map.of("type", "SOS_ALERT", "incidentId", incident.getId()),
                        "emergency");
            }
        } catch (Exception e) {
            log.error("HealthGuard: escalation push failed for incident {}: {}", incident.getId(), e.getMessage());
        }
    }
}
