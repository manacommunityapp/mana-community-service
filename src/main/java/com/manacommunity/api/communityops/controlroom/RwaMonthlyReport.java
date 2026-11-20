package com.manacommunity.api.communityops.controlroom;

import java.util.Map;

public record RwaMonthlyReport(
    int year,
    int month,
    long totalStaffShifts,
    long presentDays,
    long absentDays,
    long parcelsReceived,
    long parcelsPickedUp,
    long moveInsCompleted,
    long moveOutsCompleted,
    long digitalPassesIssued,
    long maintenanceLogsCompleted,
    long maintenanceLogsCost,
    Map<String, Long> attendanceByStatus
) {}
