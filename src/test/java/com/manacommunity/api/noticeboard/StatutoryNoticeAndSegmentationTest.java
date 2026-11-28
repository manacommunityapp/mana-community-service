package com.manacommunity.api.noticeboard;

import com.manacommunity.api.noticeboard.statutory.entity.AudienceGroup;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceMember;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceMember.OwnershipType;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceRule;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceRule.RuleConjunction;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceRule.RuleField;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceRule.RuleOperator;
import com.manacommunity.api.noticeboard.statutory.entity.DeliveryChannel;
import com.manacommunity.api.noticeboard.statutory.entity.DeliveryChannel.ChannelType;
import com.manacommunity.api.noticeboard.statutory.entity.ReadReceipt;
import com.manacommunity.api.noticeboard.statutory.entity.ReadReceipt.BeaconStatus;
import com.manacommunity.api.noticeboard.statutory.entity.StatutoryNotice;
import com.manacommunity.api.noticeboard.statutory.entity.StatutoryNotice.NoticeCategory;
import com.manacommunity.api.noticeboard.statutory.entity.StatutoryNotice.NoticePriority;
import com.manacommunity.api.noticeboard.statutory.entity.StatutoryNotice.NoticeStatus;
import com.manacommunity.api.noticeboard.statutory.repository.AudienceGroupRepository;
import com.manacommunity.api.noticeboard.statutory.repository.AudienceMemberRepository;
import com.manacommunity.api.noticeboard.statutory.repository.AudienceRuleRepository;
import com.manacommunity.api.noticeboard.statutory.repository.DeliveryChannelRepository;
import com.manacommunity.api.noticeboard.statutory.repository.ReadReceiptRepository;
import com.manacommunity.api.noticeboard.statutory.repository.StatutoryNoticeRepository;
import com.manacommunity.api.noticeboard.statutory.service.AudienceSegmentationEngine;
import com.manacommunity.api.noticeboard.statutory.service.AudienceSegmentationEngine.ResidentRecord;
import com.manacommunity.api.noticeboard.statutory.service.DeliveryTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatutoryNoticeAndSegmentationTest {

    @Mock private AudienceGroupRepository groupRepo;
    @Mock private AudienceRuleRepository ruleRepo;
    @Mock private AudienceMemberRepository memberRepo;
    @Mock private StatutoryNoticeRepository noticeRepo;
    @Mock private DeliveryChannelRepository channelRepo;
    @Mock private ReadReceiptRepository receiptRepo;

    private AudienceSegmentationEngine segmentationEngine;
    private DeliveryTrackingService deliveryService;

    @BeforeEach
    void setUp() {
        segmentationEngine = new AudienceSegmentationEngine(groupRepo, ruleRepo, memberRepo);
        deliveryService = new DeliveryTrackingService(noticeRepo, channelRepo, receiptRepo, memberRepo);
    }

    @Test
    void publishes4000FlatAgmNoticeInBatchesAndTracksLegalTimestampsViaIndexedBeacon() {
        List<AudienceMember> members = new ArrayList<>(4000);
        for (int i = 1; i <= 4000; i++) {
            members.add(AudienceMember.builder()
                    .audienceGroupId(50L).communityId("COMM-1")
                    .flatNumber("F-" + i).towerName("Tower-" + ((i % 4) + 1))
                    .residentName("Resident " + i).userId((long) i).build());
        }

        StatutoryNotice agm = StatutoryNotice.builder()
                .id(77L).communityId("COMM-1").title("Annual General Meeting Summons")
                .category(NoticeCategory.AGM_SUMMONS).priority(NoticePriority.CRITICAL)
                .status(NoticeStatus.DRAFT).statutory(true).readReceiptRequired(true)
                .audienceGroupId(50L).build();

        Map<String, ReadReceipt> receiptStore = new HashMap<>();
        when(noticeRepo.findById(77L)).thenReturn(Optional.of(agm));
        when(noticeRepo.save(any(StatutoryNotice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepo.findByAudienceGroupId(50L)).thenReturn(members);
        when(channelRepo.save(any(DeliveryChannel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(receiptRepo.saveAll(anyList())).thenAnswer(inv -> {
            List<ReadReceipt> batch = inv.getArgument(0);
            for (ReadReceipt r : batch) {
                receiptStore.put(r.getBeaconTrackingId(), r);
            }
            return batch;
        });

        long t0 = System.currentTimeMillis();
        StatutoryNotice published = deliveryService.publishNotice(
                77L, List.of(ChannelType.APP_PUSH, ChannelType.EMAIL, ChannelType.WHATSAPP));
        long elapsedMs = System.currentTimeMillis() - t0;

        assertThat(published.getStatus()).isEqualTo(NoticeStatus.PUBLISHED);
        assertThat(published.getTotalRecipients()).isEqualTo(4000);
        // 4,000 flats x 3 channels = 12,000 receipts saved in 500-row batches (24 saveAll calls, 0 individual saves)
        assertThat(receiptStore).hasSize(12_000);
        verify(receiptRepo, times(24)).saveAll(anyList());
        verify(receiptRepo, never()).save(any(ReadReceipt.class));
        assertThat(elapsedMs).isLessThan(3000L);

        // Beacon lookup uses indexed findByBeaconTrackingId (NEVER findAll) and stamps SENT -> DELIVERED -> OPENED
        ReadReceipt sample = receiptStore.values().iterator().next();
        when(receiptRepo.findByBeaconTrackingId(sample.getBeaconTrackingId())).thenReturn(Optional.of(sample));
        when(receiptRepo.findByNoticeId(77L)).thenReturn(List.of(sample));
        when(receiptRepo.save(sample)).thenReturn(sample);

        deliveryService.processBeacon(sample.getBeaconTrackingId(), BeaconStatus.DELIVERED);
        assertThat(sample.getDeliveredAt()).isNotNull();

        deliveryService.processBeacon(sample.getBeaconTrackingId(), BeaconStatus.OPENED);
        assertThat(sample.getOpenedAt()).isNotNull();
        assertThat(sample.isLegallyValid()).isTrue();
        verify(receiptRepo, never()).findAll();
    }

    @Test
    void evaluatesCompoundAudienceRulesAndUpdatesDynamicallyAfterDuesPayment() {
        List<AudienceRule> rules = List.of(
                AudienceRule.builder().audienceGroupId(10L).ruleOrder(1).field(RuleField.OWNERSHIP_TYPE)
                        .operator(RuleOperator.EQUALS).value("OWNER").conjunction(RuleConjunction.AND).build(),
                AudienceRule.builder().audienceGroupId(10L).ruleOrder(2).field(RuleField.TOWER)
                        .operator(RuleOperator.IN).value("Tower A,Tower C").conjunction(RuleConjunction.AND).build(),
                AudienceRule.builder().audienceGroupId(10L).ruleOrder(3).field(RuleField.VEHICLE_TYPE)
                        .operator(RuleOperator.EQUALS).value("4_WHEELER").conjunction(RuleConjunction.AND).build(),
                AudienceRule.builder().audienceGroupId(10L).ruleOrder(4).field(RuleField.PENDING_DUES_AMOUNT)
                        .operator(RuleOperator.LESS_EQUAL).value("5000").conjunction(null).build()
        );

        AudienceGroup group = AudienceGroup.builder()
                .id(10L).communityId("COMM-1").name("AGM Eligible Tower A/C 4W Owners").dynamic(true).build();

        when(groupRepo.findById(10L)).thenReturn(Optional.of(group));
        when(groupRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ruleRepo.findByAudienceGroupIdOrderByRuleOrder(10L)).thenReturn(rules);
        when(memberRepo.findByAudienceGroupId(10L)).thenReturn(List.of());
        when(memberRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // Candidate list: only R1 qualifies initially; R2 owes ₹8,000; R3 is Tenant; R4 is Tower B
        ResidentRecord r1 = new ResidentRecord("A-101", "Tower A", "Amit", "a@m.in", "9001", 1L,
                OwnershipType.OWNER, "3BHK", "4_WHEELER", 1500.0, 24, "OCCUPIED", 1800, "P1", "", false, true, "30-45");
        ResidentRecord r2HighDues = new ResidentRecord("C-202", "Tower C", "Priya", "p@m.in", "9002", 2L,
                OwnershipType.OWNER, "3BHK", "4_WHEELER", 8000.0, 18, "OCCUPIED", 1800, "P2", "", false, true, "30-45");
        ResidentRecord r3Tenant = new ResidentRecord("A-303", "Tower A", "Kiran", "k@m.in", "9003", 3L,
                OwnershipType.TENANT, "2BHK", "4_WHEELER", 0.0, 12, "OCCUPIED", 1200, "P3", "", false, true, "25-35");
        ResidentRecord r4WrongTower = new ResidentRecord("B-404", "Tower B", "Neha", "n@m.in", "9004", 4L,
                OwnershipType.OWNER, "3BHK", "4_WHEELER", 0.0, 36, "OCCUPIED", 1800, "P4", "", false, true, "30-45");

        var initial = segmentationEngine.evaluateGroup(10L, List.of(r1, r2HighDues, r3Tenant, r4WrongTower));
        assertThat(initial.matched()).isEqualTo(1);

        // Resident R2 pays dues mid-campaign (₹8,000 -> ₹0) -> dynamically joins group on re-evaluation
        ResidentRecord r2Paid = new ResidentRecord("C-202", "Tower C", "Priya", "p@m.in", "9002", 2L,
                OwnershipType.OWNER, "3BHK", "4_WHEELER", 0.0, 18, "OCCUPIED", 1800, "P2", "", false, true, "30-45");
        var updated = segmentationEngine.evaluateGroup(10L, List.of(r1, r2Paid, r3Tenant, r4WrongTower));
        assertThat(updated.matched()).isEqualTo(2);
        assertThat(updated.towerBreakdown()).containsEntry("Tower A", 1L).containsEntry("Tower C", 1L);
    }
}
