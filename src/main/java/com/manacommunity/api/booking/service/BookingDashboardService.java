package com.manacommunity.api.booking.service;

import com.manacommunity.api.booking.dto.BookingAnalyticsResponse;
import com.manacommunity.api.booking.dto.DashboardStatsResponse;
import com.manacommunity.api.booking.dto.ResourceBookingResponse;
import com.manacommunity.api.booking.entity.BookingAnalytics;
import com.manacommunity.api.booking.entity.ResourceBooking;
import com.manacommunity.api.booking.entity.enums.BookingStatus;
import com.manacommunity.api.booking.entity.enums.ResourceStatus;
import com.manacommunity.api.booking.repository.BookingAnalyticsRepository;
import com.manacommunity.api.booking.repository.ResourceBookingRepository;
import com.manacommunity.api.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingDashboardService {

    private final ResourceRepository resourceRepo;
    private final ResourceBookingRepository bookingRepo;
    private final BookingAnalyticsRepository analyticsRepo;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Long communityId) {
        long totalResources = resourceRepo.countByCommunityIdAndDeletedFalse(communityId);

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        List<ResourceBooking> monthBookings = bookingRepo.findByCommunityIdAndBookingDateBetweenOrderByBookingDateAsc(
                communityId, monthStart, today);

        long totalBookings = monthBookings.size();
        long todayBookings = monthBookings.stream()
                .filter(b -> b.getBookingDate().equals(today))
                .count();
        long activeBookings = monthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.CHECKED_IN)
                .count();
        long cancelledBookings = monthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .count();

        BigDecimal revenue = monthBookings.stream()
                .filter(b -> b.getTotalAmount() != null && b.getStatus() != BookingStatus.CANCELLED)
                .map(ResourceBooking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double cancellationRate = totalBookings > 0
                ? (double) cancelledBookings / totalBookings * 100
                : 0.0;

        double occupancyRate = totalResources > 0
                ? Math.min((double) activeBookings / totalResources * 100, 100.0)
                : 0.0;

        List<ResourceBookingResponse> recentBookings = bookingRepo
                .findByCommunityIdAndStatusIn(communityId,
                        List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING, BookingStatus.CHECKED_IN),
                        PageRequest.of(0, 10))
                .getContent()
                .stream()
                .map(this::toBookingResponse)
                .toList();

        return DashboardStatsResponse.builder()
                .totalResources(totalResources)
                .totalBookings(totalBookings)
                .todayBookings(todayBookings)
                .activeBookings(activeBookings)
                .occupancyRate(BigDecimal.valueOf(occupancyRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .revenue(revenue)
                .cancellationRate(BigDecimal.valueOf(cancellationRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .recentBookings(recentBookings)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BookingAnalyticsResponse> getAnalytics(Long communityId, Long resourceId, LocalDate from, LocalDate to) {
        if (resourceId != null) {
            return analyticsRepo.findByResourceIdAndAnalyticsDateBetween(resourceId, from, to)
                    .stream().map(this::toAnalyticsResponse).toList();
        }
        return analyticsRepo.findByCommunityIdAndAnalyticsDateBetweenOrderByAnalyticsDateAsc(communityId, from, to)
                .stream().map(this::toAnalyticsResponse).toList();
    }

    private ResourceBookingResponse toBookingResponse(ResourceBooking b) {
        return ResourceBookingResponse.builder()
                .id(b.getId())
                .bookingNumber(b.getBookingNumber())
                .resourceId(b.getResource().getId())
                .resourceName(b.getResource().getName())
                .resourceCategory(b.getResource().getCategory() != null ? b.getResource().getCategory().getName() : null)
                .bookingDate(b.getBookingDate().toString())
                .startTime(b.getStartTime().toString())
                .endTime(b.getEndTime().toString())
                .purpose(b.getPurpose())
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .approvalStatus(b.getApprovalStatus() != null ? b.getApprovalStatus().name() : null)
                .totalAmount(b.getTotalAmount())
                .paymentStatus(b.getPaymentStatus() != null ? b.getPaymentStatus().name() : null)
                .bookedById(b.getBookedBy().getId())
                .bookedByName(b.getBookedBy().getFullName())
                .createdAt(b.getCreatedAt() != null ? b.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    private BookingAnalyticsResponse toAnalyticsResponse(BookingAnalytics a) {
        return BookingAnalyticsResponse.builder()
                .id(a.getId())
                .resourceId(a.getResource().getId())
                .resourceName(a.getResource().getName())
                .analyticsDate(a.getAnalyticsDate().toString())
                .totalBookings(a.getTotalBookings())
                .confirmedBookings(a.getConfirmedBookings())
                .cancelledBookings(a.getCancelledBookings())
                .noShows(a.getNoShows())
                .revenue(a.getRevenue())
                .occupancyPercentage(a.getOccupancyPercentage())
                .averageRating(a.getAverageRating())
                .peakHourStart(a.getPeakHourStart() != null ? a.getPeakHourStart().toString() : null)
                .peakHourEnd(a.getPeakHourEnd() != null ? a.getPeakHourEnd().toString() : null)
                .build();
    }
}
