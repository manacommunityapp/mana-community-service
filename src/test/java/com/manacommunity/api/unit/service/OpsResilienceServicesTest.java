package com.manacommunity.api.unit.service;

import com.manacommunity.api.opsresilience.entity.GuardExpressCheckin;
import com.manacommunity.api.opsresilience.entity.IncidentPostmortem;
import com.manacommunity.api.opsresilience.entity.PostmortemGuardrail;
import com.manacommunity.api.opsresilience.entity.TenantAccountingRule;
import com.manacommunity.api.opsresilience.repository.GuardExpressCheckinRepository;
import com.manacommunity.api.opsresilience.repository.IncidentPostmortemRepository;
import com.manacommunity.api.opsresilience.repository.PostmortemGuardrailRepository;
import com.manacommunity.api.opsresilience.repository.TenantAccountingRuleRepository;
import com.manacommunity.api.opsresilience.service.BlamelessPostmortemService;
import com.manacommunity.api.opsresilience.service.GuardExpressCheckinService;
import com.manacommunity.api.opsresilience.service.GuardExpressCheckinService.ExpressCheckinRequest;
import com.manacommunity.api.opsresilience.service.TenantAccountingConfigService;
import com.manacommunity.api.opsresilience.service.TenantAccountingConfigService.UpsertRuleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OpsResilienceServicesTest {

    @Mock private GuardExpressCheckinRepository checkinRepo;
    @Mock private TenantAccountingRuleRepository ruleRepo;
    @Mock private IncidentPostmortemRepository postmortemRepo;
    @Mock private PostmortemGuardrailRepository guardrailRepo;

    private GuardExpressCheckinService guardExpressService;
    private TenantAccountingConfigService accountingConfigService;
    private BlamelessPostmortemService postmortemService;

    private final Map<String, GuardExpressCheckin> checkinStore = new ConcurrentHashMap<>();
    private final Map<String, TenantAccountingRule> ruleStore = new ConcurrentHashMap<>();
    private final Map<Long, IncidentPostmortem> pmStore = new ConcurrentHashMap<>();
    private final Map<Long, PostmortemGuardrail> grStore = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1L);

    @BeforeEach
    void setUp() {
        checkinStore.clear();
        ruleStore.clear();
        pmStore.clear();
        grStore.clear();

        lenient().when(checkinRepo.findByGateIdAndClientSeqId(anyString(), anyLong())).thenAnswer(inv ->
                Optional.ofNullable(checkinStore.get(inv.getArgument(0) + "#" + inv.getArgument(1))));
        lenient().when(checkinRepo.save(any())).thenAnswer(inv -> {
            GuardExpressCheckin c = inv.getArgument(0);
            if (c.getId() == null) c.setId(seq.getAndIncrement());
            checkinStore.put(c.getGateId() + "#" + c.getClientSeqId(), c);
            return c;
        });

        lenient().when(ruleRepo.findByCommunityIdAndRuleKey(anyLong(), anyString())).thenAnswer(inv ->
                Optional.ofNullable(ruleStore.get(inv.getArgument(0) + "|" + inv.getArgument(1))));
        lenient().when(ruleRepo.save(any())).thenAnswer(inv -> {
            TenantAccountingRule r = inv.getArgument(0);
            if (r.getId() == null) r.setId(seq.getAndIncrement());
            ruleStore.put(r.getCommunityId() + "|" + r.getRuleKey(), r);
            return r;
        });

        lenient().when(postmortemRepo.save(any())).thenAnswer(inv -> {
            IncidentPostmortem p = inv.getArgument(0);
            if (p.getId() == null) p.setId(seq.getAndIncrement());
            pmStore.put(p.getId(), p);
            return p;
        });
        lenient().when(postmortemRepo.findById(anyLong())).thenAnswer(inv ->
                Optional.ofNullable(pmStore.get(inv.getArgument(0))));

        lenient().when(guardrailRepo.save(any())).thenAnswer(inv -> {
            PostmortemGuardrail g = inv.getArgument(0);
            if (g.getId() == null) g.setId(seq.getAndIncrement());
            grStore.put(g.getId(), g);
            return g;
        });
        lenient().when(guardrailRepo.findById(anyLong())).thenAnswer(inv ->
                Optional.ofNullable(grStore.get(inv.getArgument(0))));
        lenient().when(guardrailRepo.findByPostmortemIdOrderByCreatedAtAsc(anyLong())).thenAnswer(inv -> {
            Long pmId = inv.getArgument(0);
            return grStore.values().stream().filter(g -> pmId.equals(g.getPostmortemId())).toList();
        });

        guardExpressService = new GuardExpressCheckinService(checkinRepo);
        accountingConfigService = new TenantAccountingConfigService(ruleRepo);
        postmortemService = new BlamelessPostmortemService(postmortemRepo, guardrailRepo);
    }

    @Test
    void enforcesRainyGateIdempotencyAndMandatoryOverridePhotoReview() {
        var courierReq = ExpressCheckinRequest.builder()
                .communityId(412L).gateId("MAIN-1").guardId(77L).clientSeqId(1001L)
                .entryMode("COURIER_ICON").brandCode("AMAZON").flatNo("A-502")
                .build();

        var first = guardExpressService.recordExpressCheckin(courierReq);
        assertThat(first.idempotentReplay()).isFalse();
        assertThat(first.audioCue()).isEqualTo("ALLOW_CHIME");
        assertThat(first.checkin().isRequiresMcReview()).isFalse();

        // Offline FIFO retry with identical gateId+clientSeqId replays idempotently
        var replay = guardExpressService.recordExpressCheckin(courierReq);
        assertThat(replay.idempotentReplay()).isTrue();
        assertThat(replay.checkin().getId()).isEqualTo(first.checkin().getId());

        // Override mode without photo fails; with photo flags requiresMcReview + OVERRIDE_REVIEW_BEEP
        var badOverride = ExpressCheckinRequest.builder()
                .communityId(412L).gateId("MAIN-1").guardId(77L).clientSeqId(1002L)
                .entryMode("GUARD_OVERRIDE_PHOTO").flatNo("C-204")
                .build();
        assertThatThrownBy(() -> guardExpressService.recordExpressCheckin(badOverride))
                .isInstanceOf(IllegalArgumentException.class);

        var okOverride = ExpressCheckinRequest.builder()
                .communityId(412L).gateId("MAIN-1").guardId(77L).clientSeqId(1002L)
                .entryMode("GUARD_OVERRIDE_PHOTO").flatNo("C-204").photoUrl("s3://gates/override-1002.jpg")
                .build();
        var overrideRes = guardExpressService.recordExpressCheckin(okOverride);
        assertThat(overrideRes.audioCue()).isEqualTo("OVERRIDE_REVIEW_BEEP");
        assertThat(overrideRes.checkin().isRequiresMcReview()).isTrue();
    }

    @Test
    void evaluatesDeclarativeTenantAccountingRuleSafelyInDryRunAndLiveModes() {
        Instant now = Instant.now();
        accountingConfigService.upsertRule(UpsertRuleRequest.builder()
                .communityId(412L)
                .ruleKey("LATE_FEE_CAP_Q4")
                .ruleType("LATE_FEE_CAP")
                .rateBps(0)
                .flatAmountPaise(50_000L) // cap late fee at Rs 500
                .effectiveFrom(now.minus(1, ChronoUnit.DAYS))
                .effectiveTo(now.plus(30, ChronoUnit.DAYS))
                .dryRunOnly(true)
                .active(true)
                .auditReason("MC Resolution 2026-11")
                .build());

        var dryRunEval = accountingConfigService.evaluateRule(412L, "LATE_FEE_CAP_Q4", 120_000L, now);
        assertThat(dryRunEval.ruleApplied()).isTrue();
        assertThat(dryRunEval.dryRunOnly()).isTrue();
        assertThat(dryRunEval.previewAdjustedPaise()).isEqualTo(50_000L);
        assertThat(dryRunEval.effectiveCommittedPaise()).isEqualTo(120_000L);

        // Promote from dry-run preview to live enforcement without code change
        accountingConfigService.upsertRule(UpsertRuleRequest.builder()
                .communityId(412L)
                .ruleKey("LATE_FEE_CAP_Q4")
                .ruleType("LATE_FEE_CAP")
                .rateBps(0)
                .flatAmountPaise(50_000L)
                .effectiveFrom(now.minus(1, ChronoUnit.DAYS))
                .effectiveTo(now.plus(30, ChronoUnit.DAYS))
                .dryRunOnly(false)
                .active(true)
                .auditReason("Treasury Approved")
                .build());

        var liveEval = accountingConfigService.evaluateRule(412L, "LATE_FEE_CAP_Q4", 120_000L, now);
        assertThat(liveEval.dryRunOnly()).isFalse();
        assertThat(liveEval.effectiveCommittedPaise()).isEqualTo(50_000L);
    }

    @Test
    void blocksBlamelessPostmortemClosureUntilAllSystemicGuardrailsAreVerified() {
        IncidentPostmortem pm = postmortemService.openPostmortem(
                "INC-2026-88",
                "Morning rush DB connection pool saturation",
                "SEV1",
                8.5,
                "SHARED_TOMCAT_DB_POOL_STARVATION",
                "Shed bulk webhook pool + activated offline gate cache",
                Instant.now().minus(1, ChronoUnit.HOURS)
        );

        assertThatThrownBy(() -> postmortemService.closePostmortem(pm.getId()))
                .isInstanceOf(IllegalStateException.class);

        PostmortemGuardrail g1 = postmortemService.addGuardrail(
                pm.getId(), "BULKHEAD_POOL", "Isolate gate fast-path DB pool", "platform-core",
                Instant.now().plus(3, ChronoUnit.DAYS)
        );
        assertThatThrownBy(() -> postmortemService.closePostmortem(pm.getId()))
                .isInstanceOf(IllegalStateException.class);

        postmortemService.verifyGuardrail(g1.getId());
        IncidentPostmortem closed = postmortemService.closePostmortem(pm.getId());
        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(closed.getResolvedAt()).isNotNull();
    }
}
