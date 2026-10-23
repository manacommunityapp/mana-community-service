package com.manacommunity.api.unit.sync;

import com.manacommunity.api.sync.SyncChangeLog;
import com.manacommunity.api.sync.SyncEnums.*;
import com.manacommunity.api.sync.dto.SyncDtos.ClientMutationItem;
import com.manacommunity.api.sync.engine.DeltaSyncEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DeltaSyncEngine Unit Tests")
public class DeltaSyncEngineTest {

    private DeltaSyncEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DeltaSyncEngine();
    }

    @Test
    @DisplayName("Normal new offline mutation is accepted and applied")
    void testNormalMutationAccepted() {
        ClientMutationItem item = ClientMutationItem.builder()
                .clientMutationId("mut-101")
                .entityType(SyncEntityType.VISITOR_PASS)
                .operation(SyncOperation.CREATE)
                .payloadJson("{\"guestName\": \"Rajesh\"}")
                .clientTimestamp(LocalDateTime.now())
                .build();

        var decision = engine.evaluateMutation(
                item,
                Optional.empty(),
                Optional.empty(),
                ConflictResolutionStrategy.SERVER_WINS
        );

        assertEquals(SyncStatus.APPLIED, decision.status());
    }

    @Test
    @DisplayName("Idempotent duplicate mutation returns existing status")
    void testIdempotentRepeatMutation() {
        SyncChangeLog existing = SyncChangeLog.builder()
                .id(42L)
                .clientMutationId("mut-101")
                .status(SyncStatus.APPLIED)
                .entityId(500L)
                .build();

        ClientMutationItem item = ClientMutationItem.builder()
                .clientMutationId("mut-101")
                .entityType(SyncEntityType.VISITOR_PASS)
                .operation(SyncOperation.CREATE)
                .payloadJson("{}")
                .clientTimestamp(LocalDateTime.now())
                .build();

        var decision = engine.evaluateMutation(
                item,
                Optional.of(existing),
                Optional.empty(),
                ConflictResolutionStrategy.SERVER_WINS
        );

        assertEquals(SyncStatus.APPLIED, decision.status());
        assertEquals(42L, decision.serverChangeId());
        assertEquals(500L, decision.entityId());
    }

    @Test
    @DisplayName("Stale client mutation yields CONFLICT_SERVER_WINS when server record is newer")
    void testConflictServerWins() {
        LocalDateTime clientTime = LocalDateTime.now().minusHours(2);
        LocalDateTime serverTime = LocalDateTime.now().minusMinutes(10); // Server modified more recently

        ClientMutationItem item = ClientMutationItem.builder()
                .clientMutationId("mut-999")
                .entityType(SyncEntityType.HELPDESK_TICKET)
                .entityId(200L)
                .operation(SyncOperation.UPDATE)
                .payloadJson("{\"status\": \"IN_PROGRESS\"}")
                .clientTimestamp(clientTime)
                .build();

        var decision = engine.evaluateMutation(
                item,
                Optional.empty(),
                Optional.of(serverTime),
                ConflictResolutionStrategy.SERVER_WINS
        );

        assertEquals(SyncStatus.CONFLICT_SERVER_WINS, decision.status());
    }
}
