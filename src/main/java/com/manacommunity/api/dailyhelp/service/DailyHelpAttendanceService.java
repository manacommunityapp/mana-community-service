package com.manacommunity.api.dailyhelp.service;

import com.manacommunity.api.dailyhelp.dto.DailyHelpDtos.*;

import java.time.LocalDate;
import java.util.List;

public interface DailyHelpAttendanceService {

    List<FlatAttendanceReconciliationDto> reconcileGateDwellSession(GateDwellIntervalDto dwell);

    MonthlySalarySettlementDto calculateMonthlySalarySettlement(Long contractId, String billingMonth);

    List<FlatAttendanceReconciliationDto> getFlatAttendanceHistory(
            Long communityId, Long flatId, LocalDate startDate, LocalDate endDate);

    CrossSocietyStaffProfileDto getPublicCrossSocietyProfile(String staffUuid);

    StaffGateScanResultDto recordGateEntry(StaffGateScanRequest request);

    StaffGateScanResultDto recordGateExit(StaffGateScanRequest request);

    CrossSocietyStaffProfileDto registerMultiSocietyWorker(MultiSocietyAffiliationRequest request);
}
