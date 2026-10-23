package com.manacommunity.api.emergency;

import com.manacommunity.api.emergency.dto.SosIncidentResponse;
import com.manacommunity.api.emergency.dto.SosResolveRequest;
import com.manacommunity.api.emergency.dto.SosTriggerRequest;
import com.manacommunity.api.emergency.engine.GateLockdownEngine;
import com.manacommunity.api.emergency.engine.SosSlaEngine;
import com.manacommunity.api.emergency.entity.GateLockdownLog;
import com.manacommunity.api.emergency.entity.SosDispatch;
import com.manacommunity.api.emergency.entity.SosIncident;
import com.manacommunity.api.emergency.entity.SosIncident.EmergencyType;
import com.manacommunity.api.emergency.entity.SosIncident.IncidentStatus;
import com.manacommunity.api.emergency.entity.SosIncident.Severity;
import com.manacommunity.api.emergency.repository.GateLockdownLogRepository;
import com.manacommunity.api.emergency.repository.SosDispatchRepository;
import com.manacommunity.api.emergency.repository.SosIncidentRepository;
import com.manacommunity.api.emergency.service.SosIncidentService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Emergency Incident Service Unit Tests")
public class SosIncidentServiceTest {

    @Mock SosIncidentRepository incidentRepository;
    @Mock SosDispatchRepository dispatchRepository;
    @Mock GateLockdownLogRepository lockdownLogRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Spy  SosSlaEngine slaEngine;
    @Spy  GateLockdownEngine lockdownEngine;

    @InjectMocks SosIncidentService sosService;

    private Community community;
    private AppUser resident;
    private AppUser guard;

    @BeforeEach
    void setUp() {
        community = new Community();
        community.setId(1L);

        resident = new AppUser();
        resident.setId(10L);
        resident.setFullName("Priya Patel");
        resident.setPhone("9876543210");
        resident.setCommunity(community);

        guard = new AppUser();
        guard.setId(88L);
        guard.setFullName("Guard Vikram");
        guard.setCommunity(community);

        when(incidentRepository.save(any(SosIncident.class))).thenAnswer(inv -> {
            SosIncident i = inv.getArgument(0);
            if (i.getId() == null) i.setId(501L);
            return i;
        });
    }

    @Test
    @DisplayName("Should trigger SOS, initiate automatic gate lockdown for INTRUDER, and broadcast alert")
    void shouldTriggerSosWithLockdown() {
        SosTriggerRequest req = new SosTriggerRequest(
                EmergencyType.SECURITY_INTRUDER,
                Severity.CRITICAL,
                "B-402",
                "Tower B",
                18.5204,
                73.8567,
                "Balcony area",
                "Suspicious individual sighted",
                null
        );

        SosIncidentResponse res = sosService.triggerSos(resident, req);

        assertThat(res.id()).isEqualTo(501L);
        assertThat(res.status()).isEqualTo("TRIGGERED");
        assertThat(res.lockdownInitiated()).isTrue();
        assertThat(res.slaTargetSeconds()).isEqualTo(180);

        verify(lockdownLogRepository).save(any(GateLockdownLog.class));
        verify(messagingTemplate).convertAndSend(contains("/emergency/sos"), any(Object.class));
    }

    @Test
    @DisplayName("Should acknowledge SOS and advance status to ACKNOWLEDGED")
    void shouldAcknowledgeSos() {
        SosIncident incident = SosIncident.builder()
                .id(501L)
                .community(community)
                .resident(resident)
                .status(IncidentStatus.TRIGGERED)
                .triggeredAt(LocalDateTime.now())
                .build();

        when(incidentRepository.findById(501L)).thenReturn(Optional.of(incident));

        SosIncidentResponse res = sosService.acknowledgeSos(guard, 501L);

        assertThat(res.status()).isEqualTo("ACKNOWLEDGED");
        assertThat(incident.getAcknowledgedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should track guard arrival and calculate SLA breach compliance")
    void shouldRecordGuardArrival() {
        SosIncident incident = SosIncident.builder()
                .id(501L)
                .community(community)
                .resident(resident)
                .status(IncidentStatus.DISPATCHED)
                .triggeredAt(LocalDateTime.now().minusMinutes(2)) // 2 min ago (Target 3 min)
                .slaTargetSeconds(180)
                .build();

        SosDispatch dispatch = SosDispatch.builder()
                .id(1L)
                .incident(incident)
                .guard(guard)
                .dispatchedAt(LocalDateTime.now().minusMinutes(2))
                .build();

        when(incidentRepository.findById(501L)).thenReturn(Optional.of(incident));
        when(dispatchRepository.findByIncidentId(501L)).thenReturn(List.of(dispatch));

        SosIncidentResponse res = sosService.recordGuardArrival(guard, 501L, "Reached flat B-402");

        assertThat(res.status()).isEqualTo("ON_SITE");
        assertThat(dispatch.isSlaBreached()).isFalse();
        verify(dispatchRepository).save(dispatch);
    }

    @Test
    @DisplayName("Should resolve SOS and restore gate barrier operation")
    void shouldResolveSosAndRestoreGates() {
        SosIncident incident = SosIncident.builder()
                .id(501L)
                .community(community)
                .resident(resident)
                .status(IncidentStatus.ON_SITE)
                .lockdownInitiated(true)
                .triggeredAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(incidentRepository.findById(501L)).thenReturn(Optional.of(incident));

        SosResolveRequest req = new SosResolveRequest(
                IncidentStatus.RESOLVED,
                "Intruder apprehended by security team",
                true
        );

        SosIncidentResponse res = sosService.resolveSos(guard, 501L, req);

        assertThat(res.status()).isEqualTo("RESOLVED");
        assertThat(res.lockdownInitiated()).isFalse();
        verify(lockdownLogRepository).save(argThat(log -> log.getDirective() == GateLockdownLog.LockdownDirective.NORMAL_RESTORE));
    }
}