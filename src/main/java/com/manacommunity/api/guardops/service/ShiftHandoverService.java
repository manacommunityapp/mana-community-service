package com.manacommunity.api.guardops.service;

import com.manacommunity.api.guardops.entity.*;
import com.manacommunity.api.guardops.entity.HandoverChecklistItem.*;
import com.manacommunity.api.guardops.entity.ShiftHandover.HandoverStatus;
import com.manacommunity.api.guardops.entity.ShiftRoster.ShiftStatus;
import com.manacommunity.api.guardops.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private final ShiftHandoverRepository handoverRepo;
    private final HandoverChecklistItemRepository checklistRepo;
    private final ShiftRosterRepository rosterRepo;
    private final GuardProfileRepository guardRepo;
    private final GamificationEventRepository gamificationRepo;

    public record HandoverResult(ShiftHandover handover, List<HandoverChecklistItem> checklist, String message) {}

    public record HandoverDashboard(
            long totalHandoversToday, long completedHandovers, long pendingHandovers, long disputedHandovers,
            long totalOpenItems, long criticalItems, long avgHandoverMinutes,
            long totalGuardsOnDuty, long lateArrivals, long noShows,
            List<ShiftHandover> recentHandovers, List<HandoverChecklistItem> unresolvedItems,
            List<GuardProfile> topPerformers
    ) {}

    public HandoverResult initiateHandover(Long outgoingGuardId, Long incomingGuardId, String gateId, String gateName,
                                           int visitorsInside, int uncollectedPkgs, int openIncidents,
                                           int pendingDeliveries, int brokenEquipment) {

        GuardProfile outgoing = guardRepo.findById(outgoingGuardId).orElseThrow();
        GuardProfile incoming = guardRepo.findById(incomingGuardId).orElseThrow();

        ShiftHandover handover = handoverRepo.save(ShiftHandover.builder()
                .communityId(outgoing.getCommunityId())
                .outgoingGuardId(outgoingGuardId).outgoingGuardName(outgoing.getGuardName())
                .incomingGuardId(incomingGuardId).incomingGuardName(incoming.getGuardName())
                .gateId(gateId).gateName(gateName)
                .handoverInitiatedAt(LocalDateTime.now())
                .visitorsStillInside(visitorsInside).uncollectedPackages(uncollectedPkgs)
                .openIncidents(openIncidents).pendingDeliveries(pendingDeliveries)
                .brokenEquipmentCount(brokenEquipment)
                .openItemsSummary(String.format("%d visitors, %d packages, %d incidents, %d deliveries, %d equipment",
                        visitorsInside, uncollectedPkgs, openIncidents, pendingDeliveries, brokenEquipment))
                .outgoingAcknowledged(false).incomingAcknowledged(false).supervisorVerified(false)
                .status(HandoverStatus.CHECKLIST_PENDING).build());

        List<HandoverChecklistItem> items = new java.util.ArrayList<>();
        if (visitorsInside > 0) items.add(buildItem(handover.getId(), ChecklistCategory.VISITORS_INSIDE, visitorsInside + " visitors still inside", visitorsInside, visitorsInside > 10 ? ItemPriority.HIGH : ItemPriority.MEDIUM));
        if (uncollectedPkgs > 0) items.add(buildItem(handover.getId(), ChecklistCategory.UNCOLLECTED_PACKAGES, uncollectedPkgs + " packages at gate uncollected", uncollectedPkgs, ItemPriority.MEDIUM));
        if (openIncidents > 0) items.add(buildItem(handover.getId(), ChecklistCategory.OPEN_INCIDENTS, openIncidents + " unresolved incidents", openIncidents, ItemPriority.CRITICAL));
        if (pendingDeliveries > 0) items.add(buildItem(handover.getId(), ChecklistCategory.PENDING_DELIVERIES, pendingDeliveries + " pending deliveries", pendingDeliveries, ItemPriority.LOW));
        if (brokenEquipment > 0) items.add(buildItem(handover.getId(), ChecklistCategory.EQUIPMENT_STATUS, brokenEquipment + " broken equipment items", brokenEquipment, ItemPriority.CRITICAL));

        List<HandoverChecklistItem> saved = checklistRepo.saveAll(items);
        return new HandoverResult(handover, saved, "Handover initiated. Incoming guard must acknowledge all items.");
    }

    public HandoverResult acknowledgeHandover(Long handoverId, Long guardId, boolean isIncoming) {
        ShiftHandover handover = handoverRepo.findById(handoverId).orElseThrow();
        List<HandoverChecklistItem> items = checklistRepo.findByHandoverIdOrderByPriorityAsc(handoverId);

        if (isIncoming) {
            handover.setIncomingAcknowledged(true);
            items.forEach(i -> i.setIncomingAcknowledged(true));
            handover.setStatus(HandoverStatus.ACKNOWLEDGED);
        } else {
            handover.setOutgoingAcknowledged(true);
            items.forEach(i -> i.setOutgoingConfirmed(true));
        }

        if (handover.isOutgoingAcknowledged() && handover.isIncomingAcknowledged()) {
            handover.setHandoverCompletedAt(LocalDateTime.now());
            handover.setStatus(HandoverStatus.COMPLETED);

            gamificationRepo.save(GamificationEvent.builder()
                    .guardId(handover.getOutgoingGuardId()).guardName(handover.getOutgoingGuardName())
                    .communityId(handover.getCommunityId())
                    .achievementType(GamificationEvent.AchievementType.PERFECT_HANDOVER)
                    .pointsAwarded(50).description("Clean shift handover completed")
                    .earnedAt(LocalDateTime.now()).badgeLevel(GamificationEvent.BadgeLevel.NONE)
                    .notified(false).build());
        }

        handoverRepo.save(handover);
        checklistRepo.saveAll(items);
        return new HandoverResult(handover, items, isIncoming ? "Incoming guard acknowledged." : "Outgoing guard confirmed.");
    }

    public HandoverDashboard getDashboard() {
        List<ShiftHandover> all = handoverRepo.findAll();
        List<ShiftRoster> rosters = rosterRepo.findAll();
        List<GuardProfile> guards = guardRepo.findAll();

        long completed = all.stream().filter(h -> h.getStatus() == HandoverStatus.COMPLETED).count();
        long pending = all.stream().filter(h -> h.getStatus() != HandoverStatus.COMPLETED && h.getStatus() != HandoverStatus.DISPUTED).count();
        long disputed = all.stream().filter(h -> h.getStatus() == HandoverStatus.DISPUTED).count();

        List<HandoverChecklistItem> allItems = checklistRepo.findAll();
        long openItems = allItems.stream().filter(i -> i.getItemStatus() == ItemStatus.OPEN || i.getItemStatus() == ItemStatus.CARRIED_OVER).count();
        long critical = allItems.stream().filter(i -> i.getPriority() == ItemPriority.CRITICAL && i.getItemStatus() != ItemStatus.RESOLVED).count();

        long lateArrivals = rosters.stream().filter(ShiftRoster::isLateArrival).count();
        long noShows = rosters.stream().filter(r -> r.getStatus() == ShiftStatus.NO_SHOW).count();
        long onDuty = rosters.stream().filter(r -> r.getStatus() == ShiftStatus.ON_DUTY || r.getStatus() == ShiftStatus.CLOCKED_IN).count();

        List<GuardProfile> topPerformers = guards.stream()
                .sorted((a, b) -> Integer.compare(b.getGamificationPoints(), a.getGamificationPoints()))
                .limit(5).collect(Collectors.toList());

        List<HandoverChecklistItem> unresolved = allItems.stream()
                .filter(i -> i.getItemStatus() != ItemStatus.RESOLVED)
                .sorted((a, b) -> a.getPriority().compareTo(b.getPriority()))
                .limit(10).collect(Collectors.toList());

        return new HandoverDashboard(all.size(), completed, pending, disputed, openItems, critical, 12,
                onDuty, lateArrivals, noShows, all, unresolved, topPerformers);
    }

    private HandoverChecklistItem buildItem(Long handoverId, ChecklistCategory cat, String desc, int count, ItemPriority priority) {
        return HandoverChecklistItem.builder()
                .handoverId(handoverId).category(cat).itemDescription(desc).count(count)
                .priority(priority).itemStatus(ItemStatus.OPEN)
                .outgoingConfirmed(false).incomingAcknowledged(false).build();
    }
}
