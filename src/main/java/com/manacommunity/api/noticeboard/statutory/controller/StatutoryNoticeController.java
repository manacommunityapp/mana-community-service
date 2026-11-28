package com.manacommunity.api.noticeboard.statutory.controller;

import com.manacommunity.api.noticeboard.statutory.entity.*;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceGroup.GroupStatus;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceMember.OwnershipType;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceRule.*;
import com.manacommunity.api.noticeboard.statutory.entity.DeliveryChannel.*;
import com.manacommunity.api.noticeboard.statutory.entity.ReadReceipt.BeaconStatus;
import com.manacommunity.api.noticeboard.statutory.entity.StatutoryNotice.*;
import com.manacommunity.api.noticeboard.statutory.repository.*;
import com.manacommunity.api.noticeboard.statutory.service.AudienceSegmentationEngine;
import com.manacommunity.api.noticeboard.statutory.service.DeliveryTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/statutory-notices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatutoryNoticeController {

    private final DeliveryTrackingService deliveryService;
    private final AudienceSegmentationEngine audienceEngine;
    private final StatutoryNoticeRepository noticeRepo;
    private final DeliveryChannelRepository channelRepo;
    private final ReadReceiptRepository receiptRepo;
    private final AudienceGroupRepository groupRepo;
    private final AudienceRuleRepository ruleRepo;
    private final AudienceMemberRepository memberRepo;

    @GetMapping("/delivery/{noticeId}")
    public ResponseEntity<?> getDeliveryDashboard(@PathVariable Long noticeId) {
        return ResponseEntity.ok(deliveryService.getDeliveryDashboard(noticeId));
    }

    @GetMapping("/audience/dashboard")
    public ResponseEntity<?> getAudienceDashboard() {
        return ResponseEntity.ok(audienceEngine.getDashboard());
    }

    @GetMapping("/notices")
    public ResponseEntity<?> getAllNotices() {
        return ResponseEntity.ok(noticeRepo.findAll());
    }

    @GetMapping("/notices/{id}")
    public ResponseEntity<?> getNotice(@PathVariable Long id) {
        return ResponseEntity.ok(noticeRepo.findById(id));
    }

    @GetMapping("/receipts/{noticeId}")
    public ResponseEntity<?> getReceipts(@PathVariable Long noticeId) {
        return ResponseEntity.ok(receiptRepo.findByNoticeId(noticeId));
    }

    @GetMapping("/audience/groups")
    public ResponseEntity<?> getAudienceGroups() {
        return ResponseEntity.ok(groupRepo.findAll());
    }

    @GetMapping("/audience/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(memberRepo.findByAudienceGroupId(groupId));
    }

    @GetMapping("/audience/{groupId}/rules")
    public ResponseEntity<?> getGroupRules(@PathVariable Long groupId) {
        return ResponseEntity.ok(ruleRepo.findByAudienceGroupIdOrderByRuleOrder(groupId));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<?> seedDemoData() {
        // --- Audience Groups ---
        AudienceGroup allResidents = groupRepo.save(AudienceGroup.builder()
                .communityId("COMM-001").name("All Residents").description("Every flat owner and tenant")
                .dynamic(false).systemDefined(true)
                .cachedMemberCount(4000).lastEvaluatedAt(LocalDateTime.now())
                .groupStatus(GroupStatus.ACTIVE).build());

        AudienceGroup towerCD4Wheeler = groupRepo.save(AudienceGroup.builder()
                .communityId("COMM-001").name("Tower C/D — 4-Wheeler Owners, No Dues >₹5K")
                .description("Owners of Tower C & D who own 4-wheelers and have no pending dues above ₹5,000")
                .dynamic(true).systemDefined(false)
                .cachedMemberCount(0).groupStatus(GroupStatus.ACTIVE).build());

        AudienceGroup ownersOnly = groupRepo.save(AudienceGroup.builder()
                .communityId("COMM-001").name("All Flat Owners").description("Only flat owners, no tenants")
                .dynamic(true).systemDefined(false)
                .cachedMemberCount(0).groupStatus(GroupStatus.ACTIVE).build());

        AudienceGroup committeeMembers = groupRepo.save(AudienceGroup.builder()
                .communityId("COMM-001").name("Managing Committee").description("MC President, Secretary, Treasurer & members")
                .dynamic(false).systemDefined(true)
                .cachedMemberCount(9).lastEvaluatedAt(LocalDateTime.now())
                .groupStatus(GroupStatus.ACTIVE).build());

        // --- Rules for Tower C/D 4-Wheeler group ---
        ruleRepo.saveAll(List.of(
                AudienceRule.builder().audienceGroupId(towerCD4Wheeler.getId())
                        .field(RuleField.TOWER).operator(RuleOperator.IN).value("Tower C, Tower D")
                        .conjunction(RuleConjunction.AND).ruleOrder(1).build(),
                AudienceRule.builder().audienceGroupId(towerCD4Wheeler.getId())
                        .field(RuleField.VEHICLE_TYPE).operator(RuleOperator.EQUALS).value("4-Wheeler")
                        .conjunction(RuleConjunction.AND).ruleOrder(2).build(),
                AudienceRule.builder().audienceGroupId(towerCD4Wheeler.getId())
                        .field(RuleField.PENDING_DUES_AMOUNT).operator(RuleOperator.LESS_EQUAL).value("5000")
                        .conjunction(null).ruleOrder(3).build()
        ));

        // --- Rules for owners only ---
        ruleRepo.save(AudienceRule.builder().audienceGroupId(ownersOnly.getId())
                .field(RuleField.OWNERSHIP_TYPE).operator(RuleOperator.EQUALS).value("OWNER")
                .ruleOrder(1).build());

        // --- Audience Members (sample from 4000 flats) ---
        String[] towers = {"Tower A", "Tower B", "Tower C", "Tower D", "Tower E"};
        String[] families = {"Sharma", "Gupta", "Patel", "Singh", "Reddy", "Kumar", "Mehta", "Jain", "Agarwal", "Verma"};
        List<AudienceMember> members = new ArrayList<>();
        int flatNum = 100;
        for (String tower : towers) {
            for (int i = 0; i < 8; i++) {
                String flat = tower.charAt(tower.length() - 1) + "-" + (flatNum + i);
                members.add(AudienceMember.builder()
                        .audienceGroupId(allResidents.getId()).communityId("COMM-001")
                        .flatNumber(flat).towerName(tower)
                        .residentName(families[i % families.length] + " Family")
                        .email(flat.toLowerCase().replace("-", "") + "@residents.mana.app")
                        .phone("98765" + String.format("%05d", flatNum + i))
                        .userId((long) (flatNum + i))
                        .ownershipType(i % 3 == 0 ? OwnershipType.TENANT : OwnershipType.OWNER)
                        .matchedByRule(false).build());
            }
        }
        memberRepo.saveAll(members);

        // --- Statutory Notices ---
        StatutoryNotice agm = noticeRepo.save(StatutoryNotice.builder()
                .communityId("COMM-001").title("AGM Notice — Annual General Meeting FY 2026-27")
                .body("All flat owners are hereby informed that the Annual General Meeting will be held on 15th December 2026 at 10:00 AM in the community hall. Agenda includes: election of new MC, maintenance fee revision, sinking fund audit report.")
                .bodyHtml("<h2>AGM Notice</h2><p>All flat owners are hereby informed...</p>")
                .category(NoticeCategory.AGM_SUMMONS).priority(NoticePriority.CRITICAL)
                .status(NoticeStatus.PUBLISHED).statutory(true).readReceiptRequired(true)
                .publishedBy("Rajesh Kapoor").publishedByRole("RWA Secretary")
                .publishedAt(LocalDateTime.of(2026, 11, 20, 9, 0))
                .effectiveDate(LocalDate.of(2026, 12, 15)).expiryDate(LocalDate.of(2026, 12, 16))
                .attachmentUrl("/docs/agm-notice-2026.pdf").attachmentName("AGM_Notice_2026.pdf")
                .audienceGroupId(allResidents.getId()).totalRecipients(40)
                .deliveredCount(38).openedCount(32).pinned(true).displayOrder(1).build());

        StatutoryNotice waterNotice = noticeRepo.save(StatutoryNotice.builder()
                .communityId("COMM-001").title("Water Tank Cleaning — Supply Interruption 28 Nov")
                .body("Water supply will be interrupted on 28th November 2026 from 9:00 AM to 3:00 PM for annual tank cleaning and disinfection. Residents are advised to store water.")
                .category(NoticeCategory.WATER_SUPPLY).priority(NoticePriority.HIGH)
                .status(NoticeStatus.PUBLISHED).statutory(false).readReceiptRequired(true)
                .publishedBy("Facility Manager").publishedByRole("Operations")
                .publishedAt(LocalDateTime.of(2026, 11, 25, 14, 0))
                .effectiveDate(LocalDate.of(2026, 11, 28)).expiryDate(LocalDate.of(2026, 11, 29))
                .audienceGroupId(allResidents.getId()).totalRecipients(40)
                .deliveredCount(40).openedCount(28).pinned(true).displayOrder(2).build());

        StatutoryNotice parkingNotice = noticeRepo.save(StatutoryNotice.builder()
                .communityId("COMM-001").title("Revised Parking Rules — Effective 1st Dec 2026")
                .body("Following the MC resolution dated 10th Nov 2026, parking rules are revised: visitor parking limited to 4 hours, no commercial vehicles overnight, EV charging slots designated in Basement 2.")
                .category(NoticeCategory.PARKING_RULES).priority(NoticePriority.NORMAL)
                .status(NoticeStatus.PUBLISHED).statutory(true).readReceiptRequired(true)
                .publishedBy("Rajesh Kapoor").publishedByRole("RWA Secretary")
                .publishedAt(LocalDateTime.of(2026, 11, 22, 11, 0))
                .effectiveDate(LocalDate.of(2026, 12, 1))
                .audienceGroupId(towerCD4Wheeler.getId()).totalRecipients(16)
                .deliveredCount(14).openedCount(10).pinned(false).displayOrder(3).build());

        StatutoryNotice fireNotice = noticeRepo.save(StatutoryNotice.builder()
                .communityId("COMM-001").title("Fire Safety Drill — Mandatory Participation")
                .body("Annual fire safety drill will be conducted on 5th December 2026. All residents must participate. Assembly points: Tower A/B — North Garden, Tower C/D — Club House, Tower E — South Lawn.")
                .category(NoticeCategory.FIRE_SAFETY).priority(NoticePriority.HIGH)
                .status(NoticeStatus.PUBLISHED).statutory(false).readReceiptRequired(false)
                .publishedBy("Safety Officer").publishedByRole("Fire & Safety")
                .publishedAt(LocalDateTime.of(2026, 11, 26, 8, 0))
                .effectiveDate(LocalDate.of(2026, 12, 5))
                .audienceGroupId(allResidents.getId()).totalRecipients(40)
                .deliveredCount(35).openedCount(20).pinned(false).displayOrder(4).build());

        StatutoryNotice electionNotice = noticeRepo.save(StatutoryNotice.builder()
                .communityId("COMM-001").title("MC Election — Nomination Filing Open")
                .body("Nominations for Managing Committee positions (President, Secretary, Treasurer, 6 Members) are open from 1st Dec to 10th Dec 2026. Only flat owners with no pending dues are eligible.")
                .category(NoticeCategory.SOCIETY_ELECTION).priority(NoticePriority.CRITICAL)
                .status(NoticeStatus.DRAFT).statutory(true).readReceiptRequired(true)
                .publishedBy("Election Officer").publishedByRole("Appointed Officer")
                .effectiveDate(LocalDate.of(2026, 12, 1)).expiryDate(LocalDate.of(2026, 12, 10))
                .audienceGroupId(ownersOnly.getId()).totalRecipients(0)
                .deliveredCount(0).openedCount(0).pinned(false).displayOrder(5).build());

        // --- Delivery Channels ---
        channelRepo.saveAll(List.of(
                DeliveryChannel.builder().noticeId(agm.getId()).communityId("COMM-001")
                        .channelType(ChannelType.APP_PUSH).channelStatus(ChannelStatus.COMPLETED)
                        .totalSent(40).totalDelivered(38).totalOpened(30).totalFailed(2).totalBounced(0)
                        .dispatchStartedAt(LocalDateTime.of(2026, 11, 20, 9, 1))
                        .dispatchCompletedAt(LocalDateTime.of(2026, 11, 20, 9, 3))
                        .deliveryRatePercent(95.0).openRatePercent(78.9).build(),
                DeliveryChannel.builder().noticeId(agm.getId()).communityId("COMM-001")
                        .channelType(ChannelType.EMAIL).channelStatus(ChannelStatus.COMPLETED)
                        .totalSent(40).totalDelivered(37).totalOpened(25).totalFailed(1).totalBounced(2)
                        .dispatchStartedAt(LocalDateTime.of(2026, 11, 20, 9, 1))
                        .dispatchCompletedAt(LocalDateTime.of(2026, 11, 20, 9, 5))
                        .deliveryRatePercent(92.5).openRatePercent(67.6).build(),
                DeliveryChannel.builder().noticeId(agm.getId()).communityId("COMM-001")
                        .channelType(ChannelType.WHATSAPP).channelStatus(ChannelStatus.COMPLETED)
                        .totalSent(40).totalDelivered(39).totalOpened(35).totalFailed(1).totalBounced(0)
                        .dispatchStartedAt(LocalDateTime.of(2026, 11, 20, 9, 2))
                        .dispatchCompletedAt(LocalDateTime.of(2026, 11, 20, 9, 4))
                        .deliveryRatePercent(97.5).openRatePercent(89.7).build(),
                DeliveryChannel.builder().noticeId(waterNotice.getId()).communityId("COMM-001")
                        .channelType(ChannelType.APP_PUSH).channelStatus(ChannelStatus.COMPLETED)
                        .totalSent(40).totalDelivered(40).totalOpened(28).totalFailed(0).totalBounced(0)
                        .dispatchStartedAt(LocalDateTime.of(2026, 11, 25, 14, 1))
                        .dispatchCompletedAt(LocalDateTime.of(2026, 11, 25, 14, 2))
                        .deliveryRatePercent(100.0).openRatePercent(70.0).build(),
                DeliveryChannel.builder().noticeId(parkingNotice.getId()).communityId("COMM-001")
                        .channelType(ChannelType.WHATSAPP).channelStatus(ChannelStatus.PARTIAL_FAILURE)
                        .totalSent(16).totalDelivered(14).totalOpened(10).totalFailed(2).totalBounced(0)
                        .dispatchStartedAt(LocalDateTime.of(2026, 11, 22, 11, 1))
                        .dispatchCompletedAt(LocalDateTime.of(2026, 11, 22, 11, 3))
                        .deliveryRatePercent(87.5).openRatePercent(71.4).build()
        ));

        // --- Read Receipts for AGM notice ---
        List<ReadReceipt> receipts = new ArrayList<>();
        for (AudienceMember m : members) {
            BeaconStatus bs;
            int idx = members.indexOf(m);
            if (idx < 32) bs = BeaconStatus.OPENED;
            else if (idx < 38) bs = BeaconStatus.DELIVERED;
            else if (idx == 38) bs = BeaconStatus.BOUNCED;
            else bs = BeaconStatus.FAILED;

            receipts.add(ReadReceipt.builder()
                    .noticeId(agm.getId()).communityId("COMM-001")
                    .flatNumber(m.getFlatNumber()).towerName(m.getTowerName())
                    .recipientName(m.getResidentName()).recipientEmail(m.getEmail())
                    .recipientPhone(m.getPhone()).recipientUserId(m.getUserId())
                    .beaconStatus(bs)
                    .sentAt(LocalDateTime.of(2026, 11, 20, 9, 1))
                    .deliveredAt(bs.ordinal() >= BeaconStatus.DELIVERED.ordinal() ? LocalDateTime.of(2026, 11, 20, 9, 2) : null)
                    .openedAt(bs.ordinal() >= BeaconStatus.OPENED.ordinal() ? LocalDateTime.of(2026, 11, 20, 9, 5 + idx % 30) : null)
                    .deliveredVia(idx % 3 == 0 ? ChannelType.WHATSAPP : idx % 3 == 1 ? ChannelType.EMAIL : ChannelType.APP_PUSH)
                    .beaconTrackingId(UUID.randomUUID().toString())
                    .legallyValid(true)
                    .signatureHash("SHA256:" + UUID.randomUUID().toString().substring(0, 16))
                    .build());
        }
        receiptRepo.saveAll(receipts);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Demo data seeded successfully");
        result.put("notices", List.of(agm, waterNotice, parkingNotice, fireNotice, electionNotice));
        result.put("audienceGroups", List.of(allResidents, towerCD4Wheeler, ownersOnly, committeeMembers));
        result.put("readReceipts", receipts.size());
        result.put("deliveryChannels", 5);
        return ResponseEntity.ok(result);
    }
}
