package com.manacommunity.api.vendor.service;

import com.manacommunity.api.vendor.dto.VendorDashboardResponse;
import com.manacommunity.api.vendor.dto.VendorPerformanceResponse;
import com.manacommunity.api.vendor.entity.VmsBooking;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.entity.VmsWorkOrder;
import com.manacommunity.api.vendor.entity.VmsContract;
import com.manacommunity.api.vendor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VmsDashboardService {

    private final VmsVendorRepository vendorRepo;
    private final VmsBookingRepository bookingRepo;
    private final VmsWorkOrderRepository workOrderRepo;
    private final VmsContractRepository contractRepo;
    private final VmsVendorRegistrationRepository registrationRepo;
    private final VmsVendorPerformanceRepository performanceRepo;

    @Transactional(readOnly = true)
    public VendorDashboardResponse getAdminDashboard(Long communityId) {
        return VendorDashboardResponse.builder()
                .totalVendors(vendorRepo.countByCommunityIdAndStatus(communityId, VmsVendor.VendorStatus.APPROVED)
                        + vendorRepo.countByCommunityIdAndStatus(communityId, VmsVendor.VendorStatus.PENDING)
                        + vendorRepo.countByCommunityIdAndStatus(communityId, VmsVendor.VendorStatus.SUSPENDED))
                .activeVendors(vendorRepo.countByCommunityIdAndStatus(communityId, VmsVendor.VendorStatus.APPROVED))
                .pendingRegistrations(registrationRepo.findByCommunityIdAndStatus(communityId, "PENDING", Pageable.unpaged()).getTotalElements())
                .totalBookings(bookingRepo.findByCommunityId(communityId, Pageable.unpaged()).getTotalElements())
                .activeBookings(bookingRepo.countByCommunityIdAndStatus(communityId, VmsBooking.BookingStatus.CONFIRMED)
                        + bookingRepo.countByCommunityIdAndStatus(communityId, VmsBooking.BookingStatus.IN_PROGRESS))
                .completedBookings(bookingRepo.countByCommunityIdAndStatus(communityId, VmsBooking.BookingStatus.COMPLETED))
                .openWorkOrders(workOrderRepo.countByCommunityIdAndStatus(communityId, VmsWorkOrder.WorkOrderStatus.CREATED)
                        + workOrderRepo.countByCommunityIdAndStatus(communityId, VmsWorkOrder.WorkOrderStatus.ASSIGNED)
                        + workOrderRepo.countByCommunityIdAndStatus(communityId, VmsWorkOrder.WorkOrderStatus.IN_PROGRESS))
                .activeContracts(contractRepo.findByCommunityIdAndStatus(communityId, VmsContract.ContractStatus.ACTIVE, Pageable.unpaged()).getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<VendorPerformanceResponse> getPerformanceLeaderboard(Long communityId, Pageable pageable) {
        return performanceRepo.findByCommunityIdOrderByPerformanceScoreDesc(communityId, pageable)
                .map(p -> VendorPerformanceResponse.builder()
                        .vendorId(p.getVendor().getId())
                        .businessName(p.getVendor().getBusinessName())
                        .avgRating(p.getAvgRating())
                        .totalRatings(p.getTotalRatings())
                        .totalBookings(p.getTotalBookings())
                        .completedBookings(p.getCompletedBookings())
                        .cancelledBookings(p.getCancelledBookings())
                        .onTimeCompletionRate(p.getOnTimeCompletionRate())
                        .responseTimeMinutes(p.getResponseTimeMinutes())
                        .totalRevenue(p.getTotalRevenue())
                        .performanceScore(p.getPerformanceScore())
                        .performanceTier(p.getPerformanceTier())
                        .build());
    }
}
