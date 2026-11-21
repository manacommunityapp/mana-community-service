package com.manacommunity.resident.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ResidentDashboardSnapshot(
    Long residentId,
    Long communityId,
    String residentName,
    String flatNumber,
    Instant capturedAt,
    DuesStatus dues,
    HouseholdSummary household,
    ActiveRequests activeRequests,
    UpcomingBookings bookings,
    WalletSummary wallet,
    CommunityPulse communityPulse,
    RecentActivity recentActivity,
    KycStatusSummary kyc
) {

    public record DuesStatus(
        BigDecimal outstanding,
        LocalDate nextDueDate,
        BigDecimal nextDueAmount,
        int overdueCount,
        List<PaymentRecord> paymentHistory
    ) {
        public record PaymentRecord(BigDecimal amount, LocalDate date, String status) {}
    }

    public record HouseholdSummary(
        String flatNumber,
        int familyMemberCount,
        int vehicleCount,
        int staffCount,
        int pendingVisitorRequests
    ) {}

    public record ActiveRequests(
        int openComplaints,
        int inProgressComplaints,
        int pendingServiceRequests,
        RecentComplaint mostRecentComplaint
    ) {
        public record RecentComplaint(String title, String status, int daysOpen) {}
    }

    public record UpcomingBookings(
        List<BookingItem> confirmed,
        int pendingApproval
    ) {
        public record BookingItem(String amenityName, String date, String slot) {}
    }

    public record WalletSummary(
        BigDecimal balance,
        BigDecimal creditAvailable,
        BigDecimal lastTransactionAmount,
        String lastTransactionDate,
        String lastTransactionType
    ) {}

    public record CommunityPulse(
        int activePolls,
        int upcomingEvents,
        int communityAnnouncements,
        double maintenanceFundHealthPct
    ) {}

    public record RecentActivity(List<ActivityItem> items) {
        public record ActivityItem(String type, String title, String timestamp) {}
    }

    public record KycStatusSummary(
        boolean isVerified,
        String verificationMethod,
        String expiresAt
    ) {}
}
