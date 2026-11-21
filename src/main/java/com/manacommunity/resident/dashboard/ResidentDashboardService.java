package com.manacommunity.resident.dashboard;

import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.finance.wallet.repository.CommunityResidentWalletRepository;
import com.manacommunity.api.governance.repository.GovernanceBallotRepository;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import com.manacommunity.api.kyc.repository.KycVerificationRequestRepository;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import com.manacommunity.api.parking.repository.ResidentVehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentDashboardService {

    private final AppUserRepository appUserRepo;
    private final InvoiceRepository invoiceRepo;
    private final TicketRepository ticketRepo;
    private final FamilyMemberRepository familyMemberRepo;
    private final ResidentVehicleRepository vehicleRepo;
    private final VisitorPassRepository visitorPassRepo;
    private final CommunityResidentWalletRepository walletRepo;
    private final GovernanceBallotRepository ballotRepo;
    private final CulturalEventRepository culturalEventRepo;
    private final KycVerificationRequestRepository kycRepo;

    @Transactional(readOnly = true)
    public ResidentDashboardSnapshot buildSnapshot(Long residentId) {
        AppUser user = appUserRepo.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found: " + residentId));
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        LocalDate today = LocalDate.now();

        return new ResidentDashboardSnapshot(
                residentId,
                communityId,
                user.getFullName(),
                user.getFlatNo(),
                Instant.now(),
                buildDues(residentId, today),
                buildHousehold(residentId, user),
                buildActiveRequests(residentId, communityId),
                buildBookings(residentId),
                buildWallet(residentId, communityId),
                buildCommunityPulse(communityId, today),
                buildRecentActivity(residentId),
                buildKyc(user, residentId)
        );
    }

    private ResidentDashboardSnapshot.DuesStatus buildDues(Long residentId, LocalDate today) {
        List<com.manacommunity.api.model.Invoice> invoices =
                invoiceRepo.findByResidentIdOrderByGeneratedAtDesc(residentId);

        BigDecimal outstanding = invoices.stream()
                .filter(i -> "UNPAID".equals(i.getStatus()) || "OVERDUE".equals(i.getStatus()) || "PARTIAL".equals(i.getStatus()))
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int overdueCount = (int) invoices.stream()
                .filter(i -> "OVERDUE".equals(i.getStatus())).count();

        com.manacommunity.api.model.Invoice nextDue = invoices.stream()
                .filter(i -> "UNPAID".equals(i.getStatus()))
                .filter(i -> i.getDueDate() != null && !i.getDueDate().isBefore(today))
                .min(java.util.Comparator.comparing(com.manacommunity.api.model.Invoice::getDueDate))
                .orElse(null);

        List<ResidentDashboardSnapshot.DuesStatus.PaymentRecord> history = invoices.stream()
                .filter(i -> "PAID".equals(i.getStatus()))
                .limit(5)
                .map(i -> new ResidentDashboardSnapshot.DuesStatus.PaymentRecord(
                        i.getTotalAmount(),
                        i.getDueDate() != null ? i.getDueDate() : today,
                        i.getStatus()))
                .collect(Collectors.toList());

        return new ResidentDashboardSnapshot.DuesStatus(
                outstanding,
                nextDue != null ? nextDue.getDueDate() : null,
                nextDue != null ? nextDue.getTotalAmount() : BigDecimal.ZERO,
                overdueCount,
                history
        );
    }

    private ResidentDashboardSnapshot.HouseholdSummary buildHousehold(Long residentId, AppUser user) {
        int familyCount = familyMemberRepo.findByUserIdOrderByCreatedAtAsc(residentId).size();
        int vehicleCount = (int) vehicleRepo.countByOwnerId(residentId);
        Long communityId2 = user.getCommunity() != null ? user.getCommunity().getId() : null;
        long pendingVisitors = communityId2 != null
                ? visitorPassRepo.countByCommunityIdAndStatus(communityId2, VisitorPass.PassStatus.PENDING) : 0;
        return new ResidentDashboardSnapshot.HouseholdSummary(
                user.getFlatNo(), familyCount, vehicleCount, 0, (int) pendingVisitors);
    }

    private ResidentDashboardSnapshot.ActiveRequests buildActiveRequests(Long residentId, Long communityId) {
        long open = ticketRepo.countByRaisedByIdAndStatusIn(residentId,
                List.of(Ticket.TicketStatus.OPEN));
        long inProgress = ticketRepo.countByRaisedByIdAndStatusIn(residentId,
                List.of(Ticket.TicketStatus.IN_PROGRESS));
        Ticket recent = ticketRepo.findByRaisedByIdOrderByCreatedAtDesc(residentId).stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.OPEN
                        || t.getStatus() == Ticket.TicketStatus.IN_PROGRESS)
                .findFirst().orElse(null);

        ResidentDashboardSnapshot.ActiveRequests.RecentComplaint recentComplaint = null;
        if (recent != null) {
            int daysOpen = (int) java.time.temporal.ChronoUnit.DAYS.between(
                    recent.getCreatedAt() != null ? recent.getCreatedAt().toLocalDate() : LocalDate.now(),
                    LocalDate.now());
            recentComplaint = new ResidentDashboardSnapshot.ActiveRequests.RecentComplaint(
                    recent.getSubject(), recent.getStatus().name(), daysOpen);
        }
        return new ResidentDashboardSnapshot.ActiveRequests(
                (int) open, (int) inProgress, 0, recentComplaint);
    }

    private ResidentDashboardSnapshot.UpcomingBookings buildBookings(Long residentId) {
        // No amenity booking table yet — return empty
        return new ResidentDashboardSnapshot.UpcomingBookings(List.of(), 0);
    }

    private ResidentDashboardSnapshot.WalletSummary buildWallet(Long residentId, Long communityId) {
        return walletRepo.findByResidentIdAndCommunityId(residentId, communityId)
                .map(w -> new ResidentDashboardSnapshot.WalletSummary(
                        w.getBalance(),
                        w.getCreditLimit(),
                        null, null, null))
                .orElse(new ResidentDashboardSnapshot.WalletSummary(
                        BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    }

    private ResidentDashboardSnapshot.CommunityPulse buildCommunityPulse(Long communityId, LocalDate today) {
        long activePolls = ballotRepo.countByCommunityIdAndStatus(communityId, "ACTIVE");
        long upcomingEvents = culturalEventRepo
                .findByCommunityIdOrderByDateAscStartTimeAscSortOrderAsc(communityId)
                .stream()
                .filter(e -> e.getDate() != null && !e.getDate().isBefore(today))
                .count();
        return new ResidentDashboardSnapshot.CommunityPulse(
                (int) activePolls, (int) upcomingEvents, 0, 0.0);
    }

    private ResidentDashboardSnapshot.RecentActivity buildRecentActivity(Long residentId) {
        List<ResidentDashboardSnapshot.RecentActivity.ActivityItem> items =
                ticketRepo.findByRaisedByIdOrderByCreatedAtDesc(residentId).stream()
                        .limit(5)
                        .map(t -> new ResidentDashboardSnapshot.RecentActivity.ActivityItem(
                                "TICKET", t.getSubject(),
                                t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate().toString() : ""))
                        .collect(Collectors.toList());
        return new ResidentDashboardSnapshot.RecentActivity(items);
    }

    private ResidentDashboardSnapshot.KycStatusSummary buildKyc(AppUser user, Long residentId) {
        boolean verified = "VERIFIED".equals(user.getKycStatus());
        String method = kycRepo.findByResidentIdOrderByCreatedAtDesc(residentId).stream()
                .findFirst()
                .map(k -> k.getVerificationMethod())
                .orElse("N/A");
        String expiresAt = kycRepo.findByResidentIdOrderByCreatedAtDesc(residentId).stream()
                .findFirst()
                .map(k -> k.getExpiresAt() != null ? k.getExpiresAt().toString() : null)
                .orElse(null);
        return new ResidentDashboardSnapshot.KycStatusSummary(verified, method, expiresAt);
    }
}
