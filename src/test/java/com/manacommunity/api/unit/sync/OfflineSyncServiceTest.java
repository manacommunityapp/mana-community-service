package com.manacommunity.api.unit.sync;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.sync.*;
import com.manacommunity.api.sync.SyncEnums.*;
import com.manacommunity.api.sync.dto.SyncDtos.*;
import com.manacommunity.api.sync.engine.DeltaSyncEngine;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfflineSyncService Unit Tests")
public class OfflineSyncServiceTest {

    @Mock
    private SyncChangeLogRepository changeLogRepository;

    @Mock
    private SyncCheckpointRepository checkpointRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private CommunityRepository communityRepository;

    @Spy
    private DeltaSyncEngine deltaEngine = new DeltaSyncEngine();

    @InjectMocks
    private OfflineSyncServiceImpl service;

    private AppUser mockUser;
    private Community mockCommunity;

    @BeforeEach
    void setUp() {
        mockUser = AppUser.builder().id(101L).fullName("Rahul Sharma").build();
        mockCommunity = Community.builder().id(1L).name("Mana Residency").build();
    }

    @Test
    @DisplayName("Push offline batch saves change logs and updates checkpoint")
    void testPushOfflineBatch() {
        when(userRepository.findById(101L)).thenReturn(Optional.of(mockUser));
        when(communityRepository.findById(1L)).thenReturn(Optional.of(mockCommunity));
        when(changeLogRepository.findByClientMutationId(anyString())).thenReturn(Optional.empty());
        when(changeLogRepository.save(any())).thenAnswer(i -> {
            SyncChangeLog log = i.getArgument(0);
            log.setId(10L);
            return log;
        });

        PushSyncBatchRequest req = PushSyncBatchRequest.builder()
                .userId(101L)
                .deviceId("device-mobile-xyz")
                .communityId(1L)
                .mutations(List.of(
                        ClientMutationItem.builder()
                                .clientMutationId("mut-001")
                                .entityType(SyncEntityType.VISITOR_PASS)
                                .operation(SyncOperation.CREATE)
                                .payloadJson("{\"guest\": \"Courier\"}")
                                .clientTimestamp(LocalDateTime.now())
                                .build()
                ))
                .build();

        PushSyncBatchResult result = service.processPushSync(req);

        assertNotNull(result);
        assertEquals(1, result.getAppliedCount());
        assertEquals(0, result.getConflictCount());
        verify(changeLogRepository).save(any());
        verify(checkpointRepository).save(any());
    }

    @Test
    @DisplayName("Pull delta returns new changes for community")
    void testPullDelta() {
        SyncChangeLog log = SyncChangeLog.builder()
                .id(5L)
                .entityType(SyncEntityType.SOS_TRIGGER)
                .entityId(99L)
                .operation(SyncOperation.CREATE)
                .payloadJson("{\"severity\": \"CRITICAL\"}")
                .serverTimestamp(LocalDateTime.now())
                .version(1L)
                .build();

        when(changeLogRepository.findDeltaForCommunity(1L, 0L)).thenReturn(List.of(log));

        PullSyncRequest req = PullSyncRequest.builder()
                .userId(101L)
                .deviceId("device-mobile-xyz")
                .communityId(1L)
                .sinceChangeId(0L)
                .build();

        PullSyncResponse res = service.processPullSync(req);

        assertNotNull(res);
        assertEquals(5L, res.getLatestChangeId());
        assertEquals(1, res.getChanges().size());
    }
}
