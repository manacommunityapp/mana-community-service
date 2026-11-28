package com.manacommunity.api.emergency;

import com.manacommunity.api.emergency.engine.GateLockdownEngine;
import com.manacommunity.api.emergency.engine.SosSlaEngine;
import com.manacommunity.api.emergency.entity.EvacuationBroadcast;
import com.manacommunity.api.emergency.entity.EvacuationMusterRoster;
import com.manacommunity.api.emergency.entity.SosIncident;
import com.manacommunity.api.emergency.repository.EvacuationBroadcastRepository;
import com.manacommunity.api.emergency.repository.EvacuationMusterRosterRepository;
import com.manacommunity.api.emergency.repository.GateLockdownLogRepository;
import com.manacommunity.api.emergency.repository.SosIncidentRepository;
import com.manacommunity.api.emergency.service.CompactSosAndEvacuationService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.CommunityFlatRepository;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompactSosAndEvacuationTest {

    @Mock private SosIncidentRepository incidentRepo;
    @Mock private GateLockdownLogRepository lockdownRepo;
    @Mock private EvacuationBroadcastRepository evacuationRepo;
    @Mock private EvacuationMusterRosterRepository rosterRepo;
    @Mock private VisitorPassRepository visitorPassRepo;
    @Mock private CommunityFlatRepository flatRepo;
    @Mock private FlatMembershipRepository membershipRepo;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private CompactSosAndEvacuationService service;

    @BeforeEach
    void setUp() {
        service = new CompactSosAndEvacuationService(
                incidentRepo,
                lockdownRepo,
                evacuationRepo,
                rosterRepo,
                visitorPassRepo,
                flatRepo,
                membershipRepo,
                new SosSlaEngine(),
                new GateLockdownEngine()
        );
        service.setMessagingTemplate(messagingTemplate);
    }

    @Test
    @DisplayName("Ingests <90B 2G/SMS compact lift SOS, fires guard siren override, and deduplicates dual-path retries")
    @SuppressWarnings("unchecked")
    void shouldIngestCompactLiftSosDeduplicateSmsFallbackAndTriggerGuardSiren() {
        Community comm = Community.builder().id(10L).name("Palm Meadows").build();
        AppUser resident = new AppUser();
        resident.setId(205L);
        resident.setFullName("Rohan Verma");
        resident.setPhone("+919876500001");
        resident.setCommunity(comm);

        String frame = CompactSosAndEvacuationService.encodeCompactFrame(
                10L, 205L, "C", "LIFT-2", "L", 1764252000L, "sos-lift-c2-99");
        assertThat(frame.length()).isLessThan(90);

        Map<String, SosIncident> savedByIdem = new HashMap<>();
        when(incidentRepo.findByIdempotencyKey("sos-lift-c2-99"))
                .thenAnswer(inv -> Optional.ofNullable(savedByIdem.get("sos-lift-c2-99")));
        when(incidentRepo.save(any(SosIncident.class))).thenAnswer(inv -> {
            SosIncident inc = inv.getArgument(0);
            inc.setId(8801L);
            savedByIdem.put(inc.getIdempotencyKey(), inc);
            return inc;
        });

        // 1. Primary compact 2G HTTP ingest -> creates incident & fires guard STREAM_ALARM siren override
        var first = service.ingestCompactSos(frame, "COMPACT_2G", resident, comm, 901L);
        assertThat(first.deduplicated()).isFalse();
        assertThat(first.sirenOverride()).isTrue();
        assertThat(first.nearestGuardId()).isEqualTo(901L);
        assertThat(first.payloadBytes()).isLessThan(90);
        assertThat(first.incident().emergencyType()).isEqualTo("LIFT_STUCK");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/community/10/sos/critical"), captor.capture());
        Map<String, Object> siren = (Map<String, Object>) captor.getValue();
        assertThat(siren.get("alarmAudioChannel")).isEqualTo("STREAM_ALARM");
        assertThat(siren.get("fullScreenIntent")).isEqualTo(true);
        assertThat(siren.get("targetTower")).isEqualTo("C");

        // 2. SMS fallback arrives 3 seconds later with exact same idempotencyKey -> deduplicated!
        var smsRetry = service.ingestCompactSos(frame, "SMS_FALLBACK", resident, comm, 901L);
        assertThat(smsRetry.deduplicated()).isTrue();
        assertThat(smsRetry.incident().id()).isEqualTo(8801L);
        verify(incidentRepo, times(1)).save(any(SosIncident.class));
    }

    @Test
    @DisplayName("Computes sub-2s Tower C on-premises roster (residents + staff + visitors) and tracks assembly muster")
    void shouldComputeTowerEvacuationRosterFastAndTrackMuster() {
        Community comm = Community.builder().id(10L).name("Palm Meadows").build();

        // Live presence cache hit: Maid currently clocked in at Tower C
        service.recordTowerPresenceEntry(10L, "C",
                new CompactSosAndEvacuationService.TowerOccupant("DOMESTIC_STAFF", 301L, "Sunita Maid", "+919900112233", "C-302"));

        // Visitors: one in Tower C (CHECKED_IN), one in Tower A (should NOT be alerted!)
        VisitorPass visitorC = VisitorPass.builder()
                .id(401L).visitorName("Plumber Ravi").visitorPhone("+919811112222")
                .flatNumber("C-504").status(VisitorPass.PassStatus.CHECKED_IN).build();
        VisitorPass visitorA = VisitorPass.builder()
                .id(402L).visitorName("Guest TowerA").visitorPhone("+919899998888")
                .flatNumber("A-101").status(VisitorPass.PassStatus.CHECKED_IN).build();
        when(visitorPassRepo.findByCommunityAndStatuses(eq(10L), anyList()))
                .thenReturn(List.of(visitorC, visitorA));

        // Flats & Resident in Tower C vs Tower A
        CommunityFlat flatC = CommunityFlat.builder().id(100L).community(comm).towerBlock("C").flatNumber("504").build();
        CommunityFlat flatA = CommunityFlat.builder().id(101L).community(comm).towerBlock("A").flatNumber("101").build();
        when(flatRepo.findByCommunityId(10L)).thenReturn(List.of(flatC, flatA));

        AppUser residentC = new AppUser();
        residentC.setId(501L);
        residentC.setFullName("Kavita Nair");
        residentC.setPhone("+919876543210");
        FlatMembership memC = FlatMembership.builder().flat(flatC).user(residentC).appAccessStatus("FULL_ACCESS").build();
        when(membershipRepo.findByFlatIdWithUser(100L)).thenReturn(List.of(memC));

        List<EvacuationMusterRoster> rosterStore = new ArrayList<>();
        when(evacuationRepo.save(any(EvacuationBroadcast.class))).thenAnswer(inv -> {
            EvacuationBroadcast b = inv.getArgument(0);
            b.setId(700L);
            return b;
        });
        when(rosterRepo.save(any(EvacuationMusterRoster.class))).thenAnswer(inv -> {
            EvacuationMusterRoster r = inv.getArgument(0);
            if (r.getId() == null) r.setId((long) (rosterStore.size() + 1));
            if (rosterStore.stream().noneMatch(x -> x.getId().equals(r.getId()))) rosterStore.add(r);
            return r;
        });
        when(rosterRepo.findByBroadcastIdOrderByPersonCategoryAscFlatNumberAsc(700L)).thenReturn(rosterStore);
        when(rosterRepo.findById(1L)).thenAnswer(inv -> Optional.of(rosterStore.get(0)));

        var dispatch = service.triggerTowerEvacuationBroadcast(
                10L, "C", "GAS_LEAK", "Evacuate Tower C via North Stairwell", "Cricket Lawn", 1L);

        assertThat(dispatch.computeTimeMs()).isLessThan(2000L);
        assertThat(dispatch.totalAlerted()).isEqualTo(3); // 1 resident (C) + 1 staff (C) + 1 visitor (C); Tower A excluded
        assertThat(dispatch.broadcast().getResidentsCount()).isEqualTo(1);
        assertThat(dispatch.broadcast().getStaffCount()).isEqualTo(1);
        assertThat(dispatch.broadcast().getVisitorsCount()).isEqualTo(1);
        assertThat(dispatch.smsFallbackPhones()).containsExactlyInAnyOrder("+919900112233", "+919811112222");

        // Security marks first occupant SAFE_AT_ASSEMBLY
        service.markSafeAtAssembly(1L);
        Map<String, Object> muster = service.getMusterSummary(700L);
        assertThat(muster.get("safeCount")).isEqualTo(1L);
        assertThat(muster.get("unaccountedCount")).isEqualTo(2L);
    }
}
