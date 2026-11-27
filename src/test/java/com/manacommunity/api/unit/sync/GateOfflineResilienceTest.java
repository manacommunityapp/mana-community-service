package com.manacommunity.api.unit.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.communityops.pass.DigitalPassRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.schoolbus.entity.BusTripLog;
import com.manacommunity.api.schoolbus.entity.RegisteredChild;
import com.manacommunity.api.schoolbus.repository.BusTripLogRepository;
import com.manacommunity.api.schoolbus.repository.RegisteredChildRepository;
import com.manacommunity.api.sync.*;
import com.manacommunity.api.sync.SyncEnums.SyncEntityType;
import com.manacommunity.api.sync.SyncEnums.SyncOperation;
import com.manacommunity.api.sync.SyncEnums.SyncStatus;
import com.manacommunity.api.sync.dto.SyncDtos.*;
import com.manacommunity.api.sync.engine.DeltaSyncEngine;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorAuditLogRepository;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Gate Offline Resilience, School-Bus Rush Reconciliation & Anti-Replay Tests")
class GateOfflineResilienceTest {

    @Mock private SyncChangeLogRepository changeLogRepository;
    @Mock private SyncCheckpointRepository checkpointRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private GateOfflineDeviceRepository deviceRepository;
    @Mock private GateEntryAuditRepository auditRepository;
    @Mock private VisitorPassRepository visitorPassRepository;
    @Mock private VisitorAuditLogRepository visitorAuditLogRepository;
    @Mock private DigitalPassRepository digitalPassRepository;
    @Mock private RegisteredChildRepository registeredChildRepository;
    @Mock private BusTripLogRepository busTripLogRepository;

    private DeltaSyncEngine deltaEngine;
    private GateOfflineSecurityService securityService;
    private OfflineSyncServiceImpl syncService;

    private Community community;
    private AppUser guard;
    private GateOfflineDevice tablet;

    @BeforeEach
    void setUp() {
        community = Community.builder().id(1L).name("Mana Enclave").build();
        guard = AppUser.builder().id(55L).fullName("Gate Guard").community(community).build();
        tablet = GateOfflineDevice.builder()
                .id(7L)
                .community(community)
                .deviceId("tablet-gate-a")
                .gateName("Main School Gate")
                .signingSecret("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .status("ACTIVE")
                .lastSeq(0L)
                .rosterVersion(1L)
                .build();

        deltaEngine = new DeltaSyncEngine();
        securityService = new GateOfflineSecurityService(
                deviceRepository,
                auditRepository,
                communityRepository,
                visitorPassRepository,
                visitorAuditLogRepository,
                digitalPassRepository,
                registeredChildRepository,
                busTripLogRepository,
                new ObjectMapper()
        );
        syncService = new OfflineSyncServiceImpl(
                changeLogRepository,
                checkpointRepository,
                userRepository,
                communityRepository,
                deltaEngine
        );
        ReflectionTestUtils.setField(syncService, "gateDeviceRepository", deviceRepository);
        ReflectionTestUtils.setField(syncService, "gateAuditRepository", auditRepository);
        ReflectionTestUtils.setField(syncService, "gateSecurityService", securityService);
    }

    @Test
    @DisplayName("Reconciles 40 offline school-bus approvals cleanly in batch order")
    void reconciles40OfflineSchoolBusEntriesCleanly() {
        when(userRepository.findById(55L)).thenReturn(Optional.of(guard));
        when(communityRepository.findById(1L)).thenReturn(Optional.of(community));
        when(deviceRepository.findByCommunityIdAndDeviceId(1L, "tablet-gate-a")).thenReturn(Optional.of(tablet));
        when(checkpointRepository.findByUserIdAndDeviceId(55L, "tablet-gate-a")).thenReturn(Optional.empty());
        when(changeLogRepository.findByClientMutationId(anyString())).thenReturn(Optional.empty());
        when(auditRepository.existsByCommunityIdAndPassNonce(eq(1L), anyString())).thenReturn(false);
        when(auditRepository.findByDeviceIdAndClientMutationId(eq("tablet-gate-a"), anyString())).thenReturn(Optional.empty());

        Map<Long, RegisteredChild> children = new HashMap<>();
        for (long i = 1; i <= 40; i++) {
            children.put(i, RegisteredChild.builder()
                    .id(i).name("Student " + i).community(community).busId(12L).status("ACTIVE").build());
        }
        when(registeredChildRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(children.get(inv.<Long>getArgument(0))));
        when(busTripLogRepository.findByChildIdAndTripDate(anyLong(), any(LocalDate.class))).thenReturn(Optional.empty());
        when(busTripLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        final long[] idSeq = {100L};
        when(changeLogRepository.save(any())).thenAnswer(inv -> {
            SyncChangeLog l = inv.getArgument(0);
            l.setId(idSeq[0]++);
            return l;
        });

        List<ClientMutationItem> mutations = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(15);
        for (long i = 1; i <= 40; i++) {
            String mutId = "mut-bus-" + i;
            String passCode = "SB-" + i + "-12";
            String nonce = passCode + "-step-" + i;
            String sig = GateOfflineSecurityService.hmacSha256(
                    tablet.getSigningSecret(),
                    "tablet-gate-a|" + mutId + "|" + i + "|SCHOOL_BUS_ENTRY|" + passCode + "|" + nonce);
            mutations.add(ClientMutationItem.builder()
                    .clientMutationId(mutId)
                    .entityType(SyncEntityType.SCHOOL_BUS_ENTRY)
                    .entityId(i)
                    .operation(SyncOperation.UPDATE)
                    .payloadJson("{\"childId\":" + i + ",\"direction\":\"PICKUP\",\"passCode\":\"" + passCode + "\"}")
                    .clientTimestamp(baseTime.plusSeconds(i * 15))
                    .localSeq(i)
                    .passCode(passCode)
                    .passNonce(nonce)
                    .gateName("Main School Gate")
                    .deviceSignature(sig)
                    .build());
        }

        PushSyncBatchResult result = syncService.processPushSync(PushSyncBatchRequest.builder()
                .userId(55L)
                .communityId(1L)
                .deviceId("tablet-gate-a")
                .mutations(mutations)
                .build());

        assertEquals(40, result.getTotalProcessed());
        assertEquals(40, result.getAppliedCount());
        assertEquals(0, result.getConflictCount());
        assertEquals(0, result.getReplayRejectedCount());
        assertEquals(40L, result.getNewCheckpointSeq());
        verify(busTripLogRepository, times(40)).save(any(BusTripLog.class));
        verify(auditRepository, times(40)).save(any(GateEntryAudit.class));
    }

    @Test
    @DisplayName("Blocks nonce/sequence/signature replays while preserving offline revoked-pass audit anomalies")
    void blocksReplayAttacksAndLogsRevokedOfflineConflict() {
        when(userRepository.findById(55L)).thenReturn(Optional.of(guard));
        when(communityRepository.findById(1L)).thenReturn(Optional.of(community));
        tablet.setLastSeq(40L);
        when(deviceRepository.findByCommunityIdAndDeviceId(1L, "tablet-gate-a")).thenReturn(Optional.of(tablet));
        when(checkpointRepository.findByUserIdAndDeviceId(55L, "tablet-gate-a")).thenReturn(Optional.empty());
        when(changeLogRepository.findByClientMutationId(anyString())).thenReturn(Optional.empty());
        when(auditRepository.findByDeviceIdAndClientMutationId(eq("tablet-gate-a"), anyString())).thenReturn(Optional.empty());
        when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changeLogRepository.save(any())).thenAnswer(inv -> {
            SyncChangeLog l = inv.getArgument(0);
            l.setId(500L);
            return l;
        });

        // 1) Duplicate nonce replay with seq=41
        String sig41 = GateOfflineSecurityService.hmacSha256(
                tablet.getSigningSecret(), "tablet-gate-a|mut-replay-nonce|41|VISITOR_PASS|VP100|nonce-used");
        when(auditRepository.existsByCommunityIdAndPassNonce(1L, "nonce-used")).thenReturn(true);

        // 2) Stale sequence replay (seq=12 <= 40)
        String sig12 = GateOfflineSecurityService.hmacSha256(
                tablet.getSigningSecret(), "tablet-gate-a|mut-replay-seq|12|VISITOR_PASS|VP101|nonce-fresh-1");
        when(auditRepository.existsByCommunityIdAndPassNonce(1L, "nonce-fresh-1")).thenReturn(false);

        // 3) Forged tablet signature
        when(auditRepository.existsByCommunityIdAndPassNonce(1L, "nonce-fresh-2")).thenReturn(false);

        // 4) Offline approval of pass that was REVOKED/REJECTED mid-outage -> logged as anomaly + preserved
        String sig42 = GateOfflineSecurityService.hmacSha256(
                tablet.getSigningSecret(), "tablet-gate-a|mut-revoked|42|VISITOR_PASS|VP999|nonce-fresh-3");
        when(auditRepository.existsByCommunityIdAndPassNonce(1L, "nonce-fresh-3")).thenReturn(false);
        VisitorPass revokedPass = VisitorPass.builder()
                .id(999L).passCode("VP999").visitorName("Parent Guest").community(community)
                .status(VisitorPass.PassStatus.REJECTED).build();
        when(visitorPassRepository.findById(999L)).thenReturn(Optional.of(revokedPass));

        PushSyncBatchResult res = syncService.processPushSync(PushSyncBatchRequest.builder()
                .userId(55L)
                .communityId(1L)
                .deviceId("tablet-gate-a")
                .mutations(List.of(
                        ClientMutationItem.builder()
                                .clientMutationId("mut-replay-nonce").entityType(SyncEntityType.VISITOR_PASS)
                                .entityId(100L).operation(SyncOperation.UPDATE).payloadJson("{\"action\":\"CHECK_IN\"}")
                                .clientTimestamp(LocalDateTime.now()).localSeq(41L).passCode("VP100")
                                .passNonce("nonce-used").deviceSignature(sig41).build(),
                        ClientMutationItem.builder()
                                .clientMutationId("mut-replay-seq").entityType(SyncEntityType.VISITOR_PASS)
                                .entityId(101L).operation(SyncOperation.UPDATE).payloadJson("{\"action\":\"CHECK_IN\"}")
                                .clientTimestamp(LocalDateTime.now()).localSeq(12L).passCode("VP101")
                                .passNonce("nonce-fresh-1").deviceSignature(sig12).build(),
                        ClientMutationItem.builder()
                                .clientMutationId("mut-bad-sig").entityType(SyncEntityType.VISITOR_PASS)
                                .entityId(102L).operation(SyncOperation.UPDATE).payloadJson("{\"action\":\"CHECK_IN\"}")
                                .clientTimestamp(LocalDateTime.now()).localSeq(41L).passCode("VP102")
                                .passNonce("nonce-fresh-2").deviceSignature("forged-bad-hex").build(),
                        ClientMutationItem.builder()
                                .clientMutationId("mut-revoked").entityType(SyncEntityType.VISITOR_PASS)
                                .entityId(999L).operation(SyncOperation.UPDATE).payloadJson("{\"action\":\"CHECK_IN\"}")
                                .clientTimestamp(LocalDateTime.now()).localSeq(42L).passCode("VP999")
                                .passNonce("nonce-fresh-3").deviceSignature(sig42).build()
                ))
                .build());

        assertEquals(0, res.getAppliedCount());
        assertEquals(3, res.getReplayRejectedCount());
        assertEquals(1, res.getConflictCount());
        assertEquals(4, res.getAnomalyCount());
        assertEquals(SyncStatus.REJECTED_REPLAY, res.getAcks().get(0).getStatus());
        assertEquals("DUPLICATE_PASS_NONCE_REPLAY", res.getAcks().get(0).getConflictReason());
        assertEquals("STALE_SEQUENCE_REPLAY", res.getAcks().get(1).getConflictReason());
        assertEquals("INVALID_DEVICE_SIGNATURE", res.getAcks().get(2).getConflictReason());
        assertEquals(SyncStatus.CONFLICT_ANOMALY_LOGGED, res.getAcks().get(3).getStatus());
        assertTrue(res.getAcks().get(3).isOfflineOverride());
        assertEquals("PASS_REJECTED_WHILE_OFFLINE", res.getAcks().get(3).getConflictReason());
        // Ensures revoked pass stayed REJECTED and was not reverted to CHECKED_IN
        assertEquals(VisitorPass.PassStatus.REJECTED, revokedPass.getStatus());
    }
}
