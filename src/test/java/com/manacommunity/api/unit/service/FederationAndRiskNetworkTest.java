package com.manacommunity.api.unit.service;

import com.manacommunity.api.federationrisk.entity.CityRiskSignal;
import com.manacommunity.api.federationrisk.entity.FederationSubSociety;
import com.manacommunity.api.federationrisk.entity.FederationTransitToken;
import com.manacommunity.api.federationrisk.repository.CityRiskSignalRepository;
import com.manacommunity.api.federationrisk.repository.FederationSubSocietyRepository;
import com.manacommunity.api.federationrisk.repository.FederationTransitTokenRepository;
import com.manacommunity.api.federationrisk.service.FederationGateRoutingService;
import com.manacommunity.api.federationrisk.service.FederationGateRoutingService.CrossSocietyIsolationException;
import com.manacommunity.api.federationrisk.service.PrivacyPreservingRiskNetworkService;
import com.manacommunity.api.federationrisk.service.PrivacyPreservingRiskNetworkService.RiskAssessment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
class FederationAndRiskNetworkTest {

    @Mock private FederationSubSocietyRepository subSocietyRepo;
    @Mock private FederationTransitTokenRepository transitRepo;
    @Mock private CityRiskSignalRepository signalRepo;

    private FederationGateRoutingService federationRoutingService;
    private PrivacyPreservingRiskNetworkService riskNetworkService;

    private final Map<String, FederationSubSociety> subStore = new ConcurrentHashMap<>();
    private final Map<String, FederationTransitToken> transitStore = new ConcurrentHashMap<>();
    private final Map<String, CityRiskSignal> signalStore = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1L);

    @BeforeEach
    void setUp() {
        subStore.clear();
        transitStore.clear();
        signalStore.clear();

        lenient().when(subSocietyRepo.findByFederationIdAndSocietyId(anyLong(), anyLong())).thenAnswer(inv ->
                Optional.ofNullable(subStore.get(inv.getArgument(0) + ":" + inv.getArgument(1))));
        lenient().when(subSocietyRepo.save(any())).thenAnswer(inv -> {
            FederationSubSociety s = inv.getArgument(0);
            if (s.getId() == null) s.setId(seq.getAndIncrement());
            subStore.put(s.getFederationId() + ":" + s.getSocietyId(), s);
            return s;
        });

        lenient().when(transitRepo.save(any())).thenAnswer(inv -> {
            FederationTransitToken t = inv.getArgument(0);
            if (t.getId() == null) t.setId(seq.getAndIncrement());
            transitStore.put(t.getTransitId(), t);
            return t;
        });
        lenient().when(transitRepo.findByTransitId(anyString())).thenAnswer(inv ->
                Optional.ofNullable(transitStore.get(inv.getArgument(0))));
        lenient().when(transitRepo.findByTargetSocietyIdAndConsumedAtIsNullAndValidUntilAfter(anyLong(), any()))
                .thenAnswer(inv -> {
                    Long targetSoc = inv.getArgument(0);
                    Instant now = inv.getArgument(1);
                    return transitStore.values().stream()
                            .filter(t -> targetSoc.equals(t.getTargetSocietyId()))
                            .filter(t -> t.getConsumedAt() == null && now.isBefore(t.getValidUntil()))
                            .toList();
                });

        lenient().when(signalRepo.findByCityCodeAndSubjectTokenHashAndSocietyBlindHash(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> Optional.ofNullable(
                        signalStore.get(inv.getArgument(0) + "|" + inv.getArgument(1) + "|" + inv.getArgument(2))));
        lenient().when(signalRepo.save(any())).thenAnswer(inv -> {
            CityRiskSignal c = inv.getArgument(0);
            if (c.getId() == null) c.setId(seq.getAndIncrement());
            signalStore.put(c.getCityCode() + "|" + c.getSubjectTokenHash() + "|" + c.getSocietyBlindHash(), c);
            return c;
        });
        lenient().when(signalRepo.findByCityCodeAndSubjectTokenHash(anyString(), anyString()))
                .thenAnswer(inv -> {
                    String city = inv.getArgument(0);
                    String sub = inv.getArgument(1);
                    return signalStore.values().stream()
                            .filter(s -> city.equals(s.getCityCode()) && sub.equals(s.getSubjectTokenHash()))
                            .toList();
                });

        federationRoutingService = new FederationGateRoutingService(subSocietyRepo, transitRepo);
        riskNetworkService = new PrivacyPreservingRiskNetworkService(signalRepo);
    }

    @Test
    void prewarmsScopedInnerGateWithoutCrossSocietyTransitLeakageOrConsumption() {
        federationRoutingService.registerSubSociety(10L, 201L, "Phase 1");
        federationRoutingService.registerSubSociety(10L, 202L, "Phase 2");

        Instant t0 = Instant.parse("2026-11-28T08:00:00Z");
        FederationTransitToken token = federationRoutingService.admitAtOuterGateAndPrewarmInner(
                10L, 202L, "Phase 2 · B-404", "TS09AB1234", 10, t0);

        assertThat(token.getOpaqueFlatLabel()).isEqualTo("Phase 2 · B-404");
        assertThat(token.getIdentifierHash()).doesNotContain("TS09AB1234").hasSize(64);

        // Phase 1 sees ZERO pre-warmed tokens destined for Phase 2
        assertThat(federationRoutingService.listPrewarmedForInnerGate(201L, t0.plusSeconds(60))).isEmpty();
        assertThat(federationRoutingService.listPrewarmedForInnerGate(202L, t0.plusSeconds(60))).hasSize(1);

        // Phase 1 cannot consume Phase 2's transit token
        assertThatThrownBy(() -> federationRoutingService.consumeTransitAtInnerGate(
                token.getTransitId(), 201L, t0.plusSeconds(120)))
                .isInstanceOf(CrossSocietyIsolationException.class);

        FederationTransitToken consumed = federationRoutingService.consumeTransitAtInnerGate(
                token.getTransitId(), 202L, t0.plusSeconds(180));
        assertThat(consumed.getConsumedAt()).isNotNull();
        assertThat(federationRoutingService.listPrewarmedForInnerGate(202L, t0.plusSeconds(200))).isEmpty();
    }

    @Test
    void requiresMultiSocietyBlindCorroborationExcludesLocalDisputesAndHonorsAppeals() {
        String suspectPhone = "+919876543210";
        Instant now = Instant.parse("2026-11-28T08:30:00Z");
        Instant exp = now.plusSeconds(90L * 86400L);

        // Local dispute in RWA 1 stays strictly LOCAL_ONLY and never triggers city alert
        CityRiskSignal localDispute = riskNetworkService.reportRiskSignal(
                "HYD", 101L, suspectPhone, "LOCAL_DISPUTE", true, exp);
        assertThat(localDispute.getVisibility()).isEqualTo("LOCAL_ONLY");
        assertThat(localDispute.getSubjectTokenHash()).doesNotContain("9876543210").hasSize(64);

        // Single verified DELIVERY_IMPERSONATION report (<2 threshold) still stays NONE
        riskNetworkService.reportRiskSignal("HYD", 101L, suspectPhone, "DELIVERY_IMPERSONATION", true, exp);
        RiskAssessment singleSource = riskNetworkService.assessCityRisk("HYD", suspectPhone, 2, now);
        assertThat(singleSource.riskTier()).isEqualTo("NONE");

        // Second independent verified RWA (102L) corroborates -> elevates to HIGH_RISK with redacted guidance
        riskNetworkService.reportRiskSignal("HYD", 102L, suspectPhone, "DELIVERY_IMPERSONATION", true, exp);
        RiskAssessment corroborated = riskNetworkService.assessCityRisk("HYD", suspectPhone, 2, now);
        assertThat(corroborated.riskTier()).isEqualTo("HIGH_RISK");
        assertThat(corroborated.topCategory()).isEqualTo("DELIVERY_IMPERSONATION");
        assertThat(corroborated.corroboratedSocietiesCount()).isEqualTo(2L);
        assertThat(corroborated.actionGuidance()).isEqualTo("VERIFY_GOVT_ID_AND_HOST_CALLBACK");

        // Appeal revocation immediately clears city-wide warning
        int revoked = riskNetworkService.revokeSubjectOnAppeal("HYD", suspectPhone);
        assertThat(revoked).isEqualTo(2);
        RiskAssessment postAppeal = riskNetworkService.assessCityRisk("HYD", suspectPhone, 2, now);
        assertThat(postAppeal.riskTier()).isEqualTo("NONE");
    }
}
