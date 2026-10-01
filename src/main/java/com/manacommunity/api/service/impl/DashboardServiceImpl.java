package com.manacommunity.api.service.impl;
import com.manacommunity.api.dto.dashboard.AdminDashboardStatsResponse;
import com.manacommunity.api.dto.dashboard.UserDashboardStatsResponse;

import com.manacommunity.api.booking.entity.enums.BookingStatus;
import com.manacommunity.api.booking.repository.ResourceBookingRepository;
import com.manacommunity.api.booking.repository.ResourceRepository;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import com.manacommunity.api.model.AuditLog;
import com.manacommunity.api.noticeboard.entity.Notice;
import com.manacommunity.api.noticeboard.repository.NoticeRepository;
import com.manacommunity.api.repository.AuditLogRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.ContentReportRepository;
import com.manacommunity.api.repository.ExpenseRepository;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.service.DashboardService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.entity.VmsWorkOrder;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import com.manacommunity.api.vendor.repository.VmsWorkOrderRepository;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final CommunityRepository communityRepository;
    private final EventCommunityRepository communityEventRepository;
    private final NoticeRepository noticeRepository;
    private final TicketRepository ticketRepository;
    private final VmsVendorRepository vmsVendorRepository;
    private final VmsWorkOrderRepository vmsWorkOrderRepository;
    private final VisitorPassRepository visitorPassRepository;
    private final ExpenseRepository expenseRepository;
    private final ContentReportRepository contentReportRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceBookingRepository resourceBookingRepository;
    private final AuditLogRepository auditLogRepository;

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getAdminStats(AppUser caller) {
        Long communityId = caller.getCommunity() != null ? caller.getCommunity().getId() : null;

        long totalUsers = communityId != null
                ? appUserRepository.countByCommunityId(communityId)
                : appUserRepository.count();

        long pendingKyc = communityId != null
                ? appUserRepository.countByCommunityIdAndKycStatus(communityId, "PENDING")
                : appUserRepository.countByKycStatus("PENDING");

        long verifiedUsers = communityId != null
                ? appUserRepository.countByCommunityIdAndKycStatus(communityId, "VERIFIED")
                : appUserRepository.countByKycStatus("VERIFIED");

        long totalRoles = roleRepository.count();
        long totalCommunities = communityRepository.count();

        long activeVisitors = 0;
        long openTickets = 0;
        long inProgressTickets = 0;
        long activeVendors = 0;
        long pendingWorkOrders = 0;
        long pendingExpenses = 0;
        long totalBookingResources = 0;
        long pendingContentReports = 0;
        long activeEvents = 0;
        long activeNotices = 0;

        if (communityId != null) {
            activeVisitors = visitorPassRepository.countByCommunityIdAndStatus(communityId, VisitorPass.PassStatus.APPROVED)
                    + visitorPassRepository.countByCommunityIdAndStatus(communityId, VisitorPass.PassStatus.CHECKED_IN);

            openTickets = ticketRepository.countByCommunityIdAndStatus(communityId, Ticket.TicketStatus.OPEN);
            inProgressTickets = ticketRepository.countByCommunityIdAndStatus(communityId, Ticket.TicketStatus.IN_PROGRESS);

            activeVendors = vmsVendorRepository.countByCommunityIdAndStatus(communityId, VmsVendor.VendorStatus.APPROVED);
            pendingWorkOrders = vmsWorkOrderRepository.countByCommunityIdAndStatus(communityId, VmsWorkOrder.WorkOrderStatus.CREATED)
                    + vmsWorkOrderRepository.countByCommunityIdAndStatus(communityId, VmsWorkOrder.WorkOrderStatus.ASSIGNED);

            pendingExpenses = expenseRepository.countByCommunityIdAndStatus(communityId, "PENDING");
            totalBookingResources = resourceRepository.countByCommunityIdAndDeletedFalse(communityId);
            pendingContentReports = contentReportRepository.countByCommunityIdAndStatus(communityId, "PENDING");

            activeEvents = communityEventRepository.countByCommunityId(communityId);
            activeNotices = noticeRepository.countByCommunityId(communityId);
        }

        List<AdminDashboardStatsResponse.RecentActivityItem> recentActivities = auditLogRepository
                .search(null, null, null, PageRequest.of(0, 10))
                .getContent()
                .stream()
                .map(this::toActivityItem)
                .collect(Collectors.toList());

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .pendingKycCount(pendingKyc)
                .verifiedUsersCount(verifiedUsers)
                .totalRolesCount(totalRoles)
                .totalCommunitiesCount(totalCommunities)
                .activeVisitorsCount(activeVisitors)
                .openTicketsCount(openTickets)
                .inProgressTicketsCount(inProgressTickets)
                .activeVendorsCount(activeVendors)
                .pendingWorkOrdersCount(pendingWorkOrders)
                .pendingExpensesCount(pendingExpenses)
                .totalBookingResourcesCount(totalBookingResources)
                .pendingContentReportsCount(pendingContentReports)
                .activeEventsCount(activeEvents)
                .activeNoticesCount(activeNotices)
                .recentActivities(recentActivities)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDashboardStatsResponse getUserStats(AppUser caller) {
        Long communityId = caller.getCommunity() != null ? caller.getCommunity().getId() : null;
        String communityName = caller.getCommunity() != null ? caller.getCommunity().getName() : "Community";

        long activeEvents = communityId != null
                ? communityEventRepository.countByCommunityId(communityId)
                : 0L;

        long activeNotices = communityId != null
                ? noticeRepository.countByCommunityId(communityId)
                : 0L;

        long myBookings = resourceBookingRepository.countByBookedByIdAndStatusIn(
                caller.getId(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

        long myTickets = ticketRepository.countByRaisedByIdAndStatusIn(
                caller.getId(),
                List.of(Ticket.TicketStatus.OPEN, Ticket.TicketStatus.IN_PROGRESS));

        List<UserDashboardStatsResponse.QuickNotice> recentNotices = List.of();
        if (communityId != null) {
            recentNotices = noticeRepository
                    .findActiveByCommunity(communityId, LocalDate.now())
                    .stream()
                    .limit(5)
                    .map(n -> UserDashboardStatsResponse.QuickNotice.builder()
                            .id(n.getId())
                            .title(n.getTitle())
                            .category(n.getCategory().name())
                            .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().format(ISO_FMT) : null)
                            .build())
                    .collect(Collectors.toList());
        }

        return UserDashboardStatsResponse.builder()
                .userName(caller.getFullName() != null ? caller.getFullName() : caller.getEmail())
                .communityName(communityName)
                .activeEventsCount(activeEvents)
                .activeNoticesCount(activeNotices)
                .myBookingsCount(myBookings)
                .myTicketsCount(myTickets)
                .recentNotices(recentNotices)
                .build();
    }

    private AdminDashboardStatsResponse.RecentActivityItem toActivityItem(AuditLog log) {
        String title = log.getAction();
        if (log.getEntityName() != null) {
            title = log.getAction() + " — " + log.getEntityName();
        }
        return AdminDashboardStatsResponse.RecentActivityItem.builder()
                .title(title)
                .timestamp(log.getCreatedAt() != null ? log.getCreatedAt().format(ISO_FMT) : null)
                .type(log.getAction())
                .module(log.getModule())
                .build();
    }
}
