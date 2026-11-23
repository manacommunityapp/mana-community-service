package com.manacommunity.api.communityops.dashboard;

import com.manacommunity.api.guard.entity.GuardShift;
import com.manacommunity.api.guard.repository.GuardShiftRepository;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import com.manacommunity.api.homeservices.model.StaffAttendance;
import com.manacommunity.api.homeservices.repository.StaffAttendanceRepository;
import com.manacommunity.api.parking.repository.ParkingSessionRepository;
import com.manacommunity.api.parking.repository.ParkingSlotRepository;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CommunityOpsDashboardService {

    private final TicketRepository ticketRepo;
    private final InvoiceRepository invoiceRepo;
    private final GuardShiftRepository guardShiftRepo;
    private final VisitorPassRepository visitorPassRepo;
    private final StaffAttendanceRepository staffAttendanceRepo;
    private final ParkingSessionRepository parkingSessionRepo;
    private final ParkingSlotRepository parkingSlotRepo;

    @Transactional(readOnly = true)
    public CommunityOpsSnapshot buildSnapshot(Long communityId) {
        LocalDate today = LocalDate.now();

        long openComplaints = ticketRepo.countByCommunityIdAndStatus(communityId, Ticket.TicketStatus.OPEN);

        long overduePayments = invoiceRepo.countOverdue(communityId, today);
        java.math.BigDecimal overduePaymentsAmount = invoiceRepo.sumOverdueAmount(communityId, today);

        long activeGuards = guardShiftRepo
                .findByCommunityIdAndShiftDateOrderByStartTimeAsc(communityId, today)
                .stream()
                .filter(s -> s.getStatus() == GuardShift.ShiftStatus.IN_PROGRESS)
                .count();
        long totalGuards = guardShiftRepo.countByCommunityIdAndShiftDate(communityId, today);

        long todayVisitors = visitorPassRepo.countByCommunityIdAndStatus(communityId, VisitorPass.PassStatus.CHECKED_IN);

        long staffPresent = staffAttendanceRepo
                .findByStaffCommunityIdAndDateOrderByStaffNameAsc(communityId, today)
                .stream()
                .filter(a -> a.getStatus() == StaffAttendance.AttendanceStatus.CHECKED_IN)
                .count();

        long parkingOccupied = parkingSessionRepo.countByCommunityIdAndExitTimeIsNull(communityId);
        long parkingTotal = parkingSlotRepo.countByCommunityId(communityId);

        return new CommunityOpsSnapshot(
                communityId,
                openComplaints,
                overduePayments,
                overduePaymentsAmount,
                activeGuards,
                totalGuards,
                todayVisitors,
                staffPresent,
                parkingOccupied,
                parkingTotal,
                Math.max(0L, parkingTotal - parkingOccupied),
                Instant.now()
        );
    }
}
