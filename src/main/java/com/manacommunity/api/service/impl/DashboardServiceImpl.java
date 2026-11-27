package com.manacommunity.api.service.impl;
import com.manacommunity.api.dto.dashboard.AdminDashboardStatsResponse;
import com.manacommunity.api.dto.dashboard.UserDashboardStatsResponse;
import com.manacommunity.api.dto.dashboard.AdminDashboardStatsResponse.BudgetSummary;

import com.manacommunity.api.booking.entity.Resource;
import com.manacommunity.api.booking.entity.ResourceBooking;
import com.manacommunity.api.booking.entity.ResourceSchedule;
import com.manacommunity.api.booking.entity.enums.BookingStatus;
import com.manacommunity.api.booking.entity.enums.ResourceStatus;
import com.manacommunity.api.booking.repository.ResourceBookingRepository;
import com.manacommunity.api.booking.repository.ResourceHolidayRepository;
import com.manacommunity.api.booking.repository.ResourceRepository;
import com.manacommunity.api.booking.repository.ResourceScheduleRepository;
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
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.repository.UserEngagementScoreRepository;
import com.manacommunity.api.repository.BudgetAllocationRepository;
import com.manacommunity.api.repository.PostRepository;
import com.manacommunity.api.model.BudgetAllocation;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.service.DashboardService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.entity.VmsWorkOrder;
import com.manacommunity.api.vendor.entity.VmsContract;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import com.manacommunity.api.vendor.repository.VmsWorkOrderRepository;
import com.manacommunity.api.vendor.repository.VmsContractRepository;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    private final ResourceScheduleRepository resourceScheduleRepository;
    private final ResourceHolidayRepository resourceHolidayRepository;
    private final AuditLogRepository auditLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final VmsContractRepository contractRepository;
    private final UserEngagementScoreRepository engagementScoreRepository;
    private final BudgetAllocationRepository budgetAllocationRepository;
    private final EventBookingRegistrationRepository eventBookingRegistrationRepository;
    private final PostRepository postRepository;

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
        long expectedVisitorsToday = 0;
        Double facilityUtilizationPercent = null;
        long openTickets = 0;
        long inProgressTickets = 0;
        long activeVendors = 0;
        long pendingWorkOrders = 0;
        long pendingExpenses = 0;
        long totalBookingResources = 0;
        long pendingContentReports = 0;
        long activeEvents = 0;
        long activeNotices = 0;
        long ownerCount = 0;
        long tenantCount = 0;
        long overdueInvoices = 0;
        BigDecimal outstandingDues = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal pendingExpenseAmount = BigDecimal.ZERO;
        long breachedSlaTickets = 0;
        long facilityBookingsToday = 0;
        long contractRenewalsDue = 0;
        long eventAttendance = 0;
        long engagedResidents = 0;
        List<BudgetSummary> budgetSummaries = List.of();
        long outstandingInvoiceCount = 0;
        BigDecimal maintenanceCollectedThisMonth = BigDecimal.ZERO;
        long monthlyFacilityBookings = 0;
        long activeVendorContracts = 0;
        BigDecimal monthlyApprovedExpense = BigDecimal.ZERO;
        BigDecimal currentYearBudget = BigDecimal.ZERO;
        BigDecimal currentYearSpent = BigDecimal.ZERO;
        long communityPostsLast30Days = 0;
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = YearMonth.from(today).atEndOfMonth();
        LocalDateTime monthStartTime = monthStart.atStartOfDay();
        LocalDateTime monthEndTime = monthEnd.plusDays(1).atStartOfDay().minusNanos(1);

        if (communityId != null) {
            activeVisitors = visitorPassRepository.countByCommunityIdAndStatus(communityId, VisitorPass.PassStatus.CHECKED_IN);
            expectedVisitorsToday = visitorPassRepository.countExpectedToday(
                    communityId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());

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

            ownerCount = appUserRepository.countByCommunityIdAndOccupancyStatusIgnoreCaseAndIsActiveTrue(communityId, "Owner");
            tenantCount = appUserRepository.countByCommunityIdAndOccupancyStatusIgnoreCaseAndIsActiveTrue(communityId, "Tenant");
            overdueInvoices = invoiceRepository.countOverdue(communityId, LocalDate.now());
            outstandingDues = invoiceRepository.sumOutstandingAmount(communityId);
            outstandingInvoiceCount = invoiceRepository.countOutstanding(communityId);
            maintenanceCollectedThisMonth = invoiceRepository.sumCollectedBetween(communityId, monthStartTime, monthEndTime);
            totalExpenses = expenseRepository.sumAmountByCommunity(communityId);
            pendingExpenseAmount = expenseRepository.sumAmountByCommunityAndStatus(communityId, "PENDING");
            monthlyApprovedExpense = expenseRepository.sumApprovedAmountByCommunityAndCreatedAtBetween(
                    communityId, monthStartTime, monthEndTime);
            breachedSlaTickets = ticketRepository.countByCommunityIdAndSlaStatusAndStatusIn(
                    communityId, Ticket.SlaStatus.BREACHED,
                    List.of(Ticket.TicketStatus.OPEN, Ticket.TicketStatus.IN_PROGRESS));
            facilityBookingsToday = resourceBookingRepository.countFacilityBookingsForDate(
                    communityId, LocalDate.now(), List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN));
            monthlyFacilityBookings = resourceBookingRepository.countFacilityBookingsForPeriod(
                    communityId, monthStart, today,
                    List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED));
            facilityUtilizationPercent = calculateFacilityUtilizationPercent(communityId, monthStart, today);
            contractRenewalsDue = contractRepository.countRenewalsDue(
                    communityId, LocalDate.now(), LocalDate.now().plusDays(30));
            activeVendorContracts = contractRepository.countByCommunityIdAndStatus(
                    communityId, VmsContract.ContractStatus.ACTIVE);
            eventAttendance = eventBookingRegistrationRepository.countByCommunityIdAndStatusNot(communityId, "CANCELLED");
            engagedResidents = engagementScoreRepository.countActiveSince(communityId, LocalDateTime.now().minusDays(30));
            budgetSummaries = buildBudgetSummaries(communityId);
            for (BudgetAllocation allocation : budgetAllocationRepository
                    .findByCommunityIdAndFinancialYearOrderByCategoryAsc(communityId, currentFinancialYear(today))) {
                currentYearBudget = currentYearBudget.add(allocation.getAllocatedAmount());
                currentYearSpent = currentYearSpent.add(allocation.getSpentAmount());
            }
            communityPostsLast30Days = postRepository.countByCommunityIdAndDeletedFalseAndCreatedAtAfter(
                    communityId, LocalDateTime.now().minusDays(30));
        }

        List<AuditLog> recentAuditLogs = communityId != null
                ? auditLogRepository.findByTenantIdOrderByCreatedAtDesc("community_" + communityId, PageRequest.of(0, 10)).getContent()
                : auditLogRepository.search(null, null, null, PageRequest.of(0, 10)).getContent();
        List<AdminDashboardStatsResponse.RecentActivityItem> recentActivities = recentAuditLogs.stream()
                .map(this::toActivityItem)
                .collect(Collectors.toList());

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .pendingKycCount(pendingKyc)
                .verifiedUsersCount(verifiedUsers)
                .totalRolesCount(totalRoles)
                .totalCommunitiesCount(totalCommunities)
                .activeVisitorsCount(activeVisitors)
                .expectedVisitorsTodayCount(expectedVisitorsToday)
                .facilityUtilizationPercent(facilityUtilizationPercent)
                .openTicketsCount(openTickets)
                .inProgressTicketsCount(inProgressTickets)
                .activeVendorsCount(activeVendors)
                .pendingWorkOrdersCount(pendingWorkOrders)
                .pendingExpensesCount(pendingExpenses)
                .totalBookingResourcesCount(totalBookingResources)
                .pendingContentReportsCount(pendingContentReports)
                .activeEventsCount(activeEvents)
                .activeNoticesCount(activeNotices)
                .ownerCount(ownerCount)
                .tenantCount(tenantCount)
                .overdueInvoicesCount(overdueInvoices)
                .outstandingDuesAmount(outstandingDues)
                .totalExpensesAmount(totalExpenses)
                .pendingExpensesAmount(pendingExpenseAmount)
                .breachedSlaTicketsCount(breachedSlaTickets)
                .facilityBookingsTodayCount(facilityBookingsToday)
                .contractRenewalsDueNext30DaysCount(contractRenewalsDue)
                .eventAttendanceCount(eventAttendance)
                .engagedResidentsLast30DaysCount(engagedResidents)
                .budgetSummaries(budgetSummaries)
                .residentCount(communityId != null ? ownerCount + tenantCount : totalUsers)
                .outstandingInvoiceCount(outstandingInvoiceCount)
                .maintenanceCollectedThisMonthAmount(maintenanceCollectedThisMonth)
                .slaBreachedTicketsCount(breachedSlaTickets)
                .monthlyFacilityBookingsCount(monthlyFacilityBookings)
                .activeVendorContractsCount(activeVendorContracts)
                .contractsExpiringWithin30DaysCount(contractRenewalsDue)
                .monthlyApprovedExpenseAmount(monthlyApprovedExpense)
                .currentFinancialYearBudgetAmount(currentYearBudget)
                .currentFinancialYearSpentAmount(currentYearSpent)
                .communityPostsLast30DaysCount(communityPostsLast30Days)
                .recentActivities(recentActivities)
                .build();
    }

    private Double calculateFacilityUtilizationPercent(Long communityId, LocalDate from, LocalDate to) {
        List<Resource> resources = resourceRepository.findByCommunityIdAndStatusAndDeletedFalseOrderByNameAsc(
                communityId, ResourceStatus.ACTIVE);
        if (resources.isEmpty()) return null;

        Map<Long, List<ResourceBooking>> bookingsByResource = resourceBookingRepository
                .findByCommunityIdAndBookingDateBetweenOrderByBookingDateAsc(communityId, from, to).stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED
                        || booking.getStatus() == BookingStatus.CHECKED_IN
                        || booking.getStatus() == BookingStatus.COMPLETED)
                .collect(Collectors.groupingBy(booking -> booking.getResource().getId()));
        Map<Long, Map<DayOfWeek, ResourceSchedule>> schedulesByResource = new java.util.HashMap<>();
        for (Resource resource : resources) {
            Map<DayOfWeek, ResourceSchedule> scheduleByDay = new java.util.EnumMap<>(DayOfWeek.class);
            resourceScheduleRepository.findByResourceIdOrderByDayOfWeekAsc(resource.getId())
                    .forEach(schedule -> scheduleByDay.put(schedule.getDayOfWeek(), schedule));
            schedulesByResource.put(resource.getId(), scheduleByDay);
        }
        java.util.Set<String> holidays = resourceHolidayRepository.findByCommunityIdAndHolidayDateBetween(communityId, from, to)
                .stream().map(holiday -> holiday.getResource().getId() + ":" + holiday.getHolidayDate())
                .collect(Collectors.toSet());

        long availableMinutes = 0;
        long bookedMinutes = 0;
        for (Resource resource : resources) {
            Map<DayOfWeek, ResourceSchedule> schedules = schedulesByResource.get(resource.getId());
            List<ResourceBooking> bookings = bookingsByResource.getOrDefault(resource.getId(), List.of());
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                if (holidays.contains(resource.getId() + ":" + date)) continue;
                ResourceSchedule schedule = schedules.get(date.getDayOfWeek());
                if (!schedules.isEmpty() && (schedule == null || !schedule.isAvailable())) continue;
                LocalTime opens = schedule != null && schedule.getOpenTime() != null
                        ? schedule.getOpenTime() : resource.getOpenTime();
                LocalTime closes = schedule != null && schedule.getCloseTime() != null
                        ? schedule.getCloseTime() : resource.getCloseTime();
                if (opens == null || closes == null || !closes.isAfter(opens)) continue;
                availableMinutes += Duration.between(opens, closes).toMinutes();
                for (ResourceBooking booking : bookings) {
                    if (!date.equals(booking.getBookingDate()) || booking.getStartTime() == null || booking.getEndTime() == null) continue;
                    LocalTime bookedStart = booking.getStartTime().isAfter(opens) ? booking.getStartTime() : opens;
                    LocalTime bookedEnd = booking.getEndTime().isBefore(closes) ? booking.getEndTime() : closes;
                    if (bookedEnd.isAfter(bookedStart)) bookedMinutes += Duration.between(bookedStart, bookedEnd).toMinutes();
                }
            }
        }
        if (availableMinutes == 0) return null;
        return BigDecimal.valueOf(bookedMinutes).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(availableMinutes), 1, java.math.RoundingMode.HALF_UP).doubleValue();
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

    private List<BudgetSummary> buildBudgetSummaries(Long communityId) {
        java.util.Map<String, BudgetSummary> summaries = new java.util.TreeMap<>();
        for (BudgetAllocation allocation : budgetAllocationRepository.findByCommunityIdOrderByCategoryAsc(communityId)) {
            BudgetSummary current = summaries.getOrDefault(allocation.getFinancialYear(),
                    BudgetSummary.builder()
                            .financialYear(allocation.getFinancialYear())
                            .allocatedAmount(BigDecimal.ZERO)
                            .spentAmount(BigDecimal.ZERO)
                            .build());
            summaries.put(allocation.getFinancialYear(), BudgetSummary.builder()
                    .financialYear(allocation.getFinancialYear())
                    .allocatedAmount(current.getAllocatedAmount().add(allocation.getAllocatedAmount()))
                    .spentAmount(current.getSpentAmount().add(allocation.getSpentAmount()))
                    .build());
        }
        return List.copyOf(summaries.values());
    }

    private String currentFinancialYear(LocalDate date) {
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return "FY " + startYear + "-" + String.format("%02d", (startYear + 1) % 100);
    }
}
