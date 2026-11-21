package com.manacommunity.api.communityops.manager;

import com.manacommunity.api.events.entity.EventCulturalEvent;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.governance.entity.CommitteeElection;
import com.manacommunity.api.governance.repository.CommitteeElectionRepository;
import com.manacommunity.api.governance.repository.CommitteePositionRepository;
import com.manacommunity.api.governance.repository.GovernanceBallotRepository;
import com.manacommunity.api.governance.repository.GovernanceProposalRepository;
import com.manacommunity.api.governance.repository.NocRequestRepository;
import com.manacommunity.api.guard.entity.GuardShift;
import com.manacommunity.api.guard.repository.GuardShiftRepository;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import com.manacommunity.api.homeservices.model.StaffAttendance;
import com.manacommunity.api.homeservices.repository.StaffAttendanceRepository;
import com.manacommunity.api.platform.audit.AuditCategory;
import com.manacommunity.api.platform.audit.AuditJournalRepository;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityManagerService {

    private final TicketRepository ticketRepo;
    private final InvoiceRepository invoiceRepo;
    private final AppUserRepository appUserRepo;
    private final GuardShiftRepository guardShiftRepo;
    private final VisitorPassRepository visitorPassRepo;
    private final StaffAttendanceRepository staffAttendanceRepo;
    private final NocRequestRepository nocRequestRepo;
    private final AuditJournalRepository auditJournalRepo;
    private final CulturalEventRepository culturalEventRepo;
    private final CommitteeElectionRepository committeeElectionRepo;
    private final CommitteePositionRepository committeePositionRepo;
    private final GovernanceBallotRepository governanceBallotRepo;
    private final GovernanceProposalRepository governanceProposalRepo;
    private final SimpMessagingTemplate messaging;

    private final ConcurrentHashMap<Long, CommunityManagerSnapshot> cache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public CommunityManagerSnapshot getSnapshot(Long communityId) {
        return cache.computeIfAbsent(communityId, this::buildSnapshot);
    }

    @Transactional(readOnly = true)
    public CommunityManagerSnapshot refreshSnapshot(Long communityId) {
        CommunityManagerSnapshot snapshot = buildSnapshot(communityId);
        cache.put(communityId, snapshot);
        messaging.convertAndSend("/topic/community/" + communityId + "/manager-dashboard", snapshot);
        return snapshot;
    }

    private CommunityManagerSnapshot buildSnapshot(Long communityId) {
        LocalDate today = LocalDate.now();

        return new CommunityManagerSnapshot(
                communityId,
                Instant.now(),
                buildMaintenance(communityId),
                buildFinance(communityId, today),
                buildResidents(communityId),
                buildSecurity(communityId, today),
                buildAmenities(),
                buildStaff(communityId, today),
                buildUpcoming(communityId, today),
                buildGovernance(communityId)
        );
    }

    private CommunityManagerSnapshot.MaintenanceStats buildMaintenance(Long communityId) {
        long open = ticketRepo.countByCommunityIdAndStatus(communityId, Ticket.TicketStatus.OPEN);
        long inProgress = ticketRepo.countByCommunityIdAndStatus(communityId, Ticket.TicketStatus.IN_PROGRESS);
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        long resolvedThisWeek = ticketRepo.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.RESOLVED)
                .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().toLocalDate().isBefore(weekAgo))
                .count();
        return new CommunityManagerSnapshot.MaintenanceStats(open, inProgress, resolvedThisWeek, 0);
    }

    private CommunityManagerSnapshot.FinanceStats buildFinance(Long communityId, LocalDate today) {
        long unpaid = invoiceRepo.countByCommunityIdAndStatus(communityId, "UNPAID");
        long overdue = invoiceRepo.countOverdue(communityId, today);
        long paidThisMonth = 0;
        try {
            java.time.LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            paidThisMonth = invoiceRepo
                    .findByCommunityIdAndGeneratedAtBetween(communityId, monthStart, now)
                    .stream()
                    .filter(i -> "PAID".equals(i.getStatus()))
                    .count();
        } catch (Exception e) {
            log.warn("Could not compute paidThisMonth for community {}: {}", communityId, e.getMessage());
        }
        java.math.BigDecimal overdueAmt = invoiceRepo
                .findByCommunityId(communityId).stream()
                .filter(i -> "OVERDUE".equals(i.getStatus()))
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return new CommunityManagerSnapshot.FinanceStats(unpaid, overdue, paidThisMonth, overdueAmt);
    }

    private CommunityManagerSnapshot.ResidentStats buildResidents(Long communityId) {
        long total = appUserRepo.countByCommunityId(communityId);
        long active = appUserRepo.countByCommunityIdAndIsActiveTrue(communityId);
        long verified = appUserRepo.countByCommunityIdAndKycStatus(communityId, "VERIFIED");
        long pending = appUserRepo.countByCommunityIdAndKycStatus(communityId, "PENDING");
        return new CommunityManagerSnapshot.ResidentStats(total, active, verified, pending);
    }

    private CommunityManagerSnapshot.SecurityStats buildSecurity(Long communityId, LocalDate today) {
        long guards = guardShiftRepo
                .findByCommunityIdAndShiftDateOrderByStartTimeAsc(communityId, today)
                .stream()
                .filter(s -> s.getStatus() == GuardShift.ShiftStatus.IN_PROGRESS)
                .count();
        long visitors = visitorPassRepo.countByCommunityIdAndStatus(communityId, VisitorPass.PassStatus.CHECKED_IN);
        long noc = nocRequestRepo.countByCommunityIdAndStatus(communityId, "PENDING");
        long secEvents = auditJournalRepo
                .findByCategoryAndCommunityId(AuditCategory.SECURITY, communityId)
                .stream()
                .filter(e -> e.getCreatedAt() != null &&
                        e.getCreatedAt().isAfter(Instant.now().minusSeconds(86400)))
                .count();
        return new CommunityManagerSnapshot.SecurityStats(guards, visitors, noc, secEvents);
    }

    private CommunityManagerSnapshot.AmenityStats buildAmenities() {
        // No amenity booking table exists yet; return zeroes
        return new CommunityManagerSnapshot.AmenityStats(0, 0);
    }

    private CommunityManagerSnapshot.StaffStats buildStaff(Long communityId, LocalDate today) {
        List<StaffAttendance> attendance =
                staffAttendanceRepo.findByStaffCommunityIdAndDateOrderByStaffNameAsc(communityId, today);
        long present = attendance.stream()
                .filter(a -> a.getStatus() == StaffAttendance.AttendanceStatus.CHECKED_IN
                        || a.getStatus() == StaffAttendance.AttendanceStatus.CHECKED_OUT)
                .count();
        long onLeave = attendance.stream()
                .filter(a -> a.getStatus() == StaffAttendance.AttendanceStatus.ON_LEAVE)
                .count();
        return new CommunityManagerSnapshot.StaffStats(present, onLeave);
    }

    private CommunityManagerSnapshot.UpcomingItems buildUpcoming(Long communityId, LocalDate today) {
        List<CommunityManagerSnapshot.UpcomingItems.EventItem> events =
                culturalEventRepo.findByCommunityIdOrderByDateAscStartTimeAscSortOrderAsc(communityId)
                        .stream()
                        .filter(e -> e.getDate() != null && !e.getDate().isBefore(today))
                        .limit(5)
                        .map(e -> new CommunityManagerSnapshot.UpcomingItems.EventItem(
                                e.getName(), e.getDate().toString()))
                        .collect(Collectors.toList());
        long pendingNoc = nocRequestRepo.countByCommunityIdAndStatus(communityId, "PENDING");
        long activeElections = committeeElectionRepo
                .findByCommunityIdAndStatus(communityId, "ACTIVE").size();
        return new CommunityManagerSnapshot.UpcomingItems(events, pendingNoc, activeElections);
    }

    private CommunityManagerSnapshot.GovernanceStats buildGovernance(Long communityId) {
        long activePolls = governanceBallotRepo.countByCommunityIdAndStatus(communityId, "ACTIVE");
        long openProposals = governanceProposalRepo.countByCommunityIdAndStatus(communityId, "OPEN");
        List<?> positions = committeePositionRepo.findByCommunityId(communityId);
        long totalPositions = positions.size();
        long filledPositions = committeeElectionRepo
                .findByCommunityIdAndStatus(communityId, "COMPLETED").size();
        return new CommunityManagerSnapshot.GovernanceStats(
                activePolls, openProposals, filledPositions, totalPositions);
    }

    @Scheduled(fixedRate = 300_000)
    public void scheduledRefresh() {
        for (Long communityId : cache.keySet()) {
            try {
                refreshSnapshot(communityId);
            } catch (Exception e) {
                log.warn("Failed refreshing community manager snapshot for {}: {}", communityId, e.getMessage());
            }
        }
    }
}
