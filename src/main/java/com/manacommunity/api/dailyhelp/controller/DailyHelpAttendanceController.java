package com.manacommunity.api.dailyhelp.controller;

import com.manacommunity.api.dailyhelp.dto.DailyHelpDtos.*;
import com.manacommunity.api.dailyhelp.service.DailyHelpAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/daily-help")
@RequiredArgsConstructor
public class DailyHelpAttendanceController {

    private final DailyHelpAttendanceService attendanceService;

    @PostMapping("/workers/affiliate")
    public ResponseEntity<CrossSocietyStaffProfileDto> registerMultiSocietyWorker(
            @RequestBody MultiSocietyAffiliationRequest request) {
        return ResponseEntity.ok(attendanceService.registerMultiSocietyWorker(request));
    }

    @GetMapping("/workers/{staffUuid}/profile")
    public ResponseEntity<CrossSocietyStaffProfileDto> getCrossSocietyProfile(
            @PathVariable String staffUuid) {
        return ResponseEntity.ok(attendanceService.getPublicCrossSocietyProfile(staffUuid));
    }

    @PostMapping("/gate/entry")
    public ResponseEntity<StaffGateScanResultDto> recordGateEntry(
            @RequestBody StaffGateScanRequest request) {
        return ResponseEntity.ok(attendanceService.recordGateEntry(request));
    }

    @PostMapping("/gate/exit")
    public ResponseEntity<StaffGateScanResultDto> recordGateExit(
            @RequestBody StaffGateScanRequest request) {
        return ResponseEntity.ok(attendanceService.recordGateExit(request));
    }

    @PostMapping("/attendance/reconcile")
    public ResponseEntity<List<FlatAttendanceReconciliationDto>> reconcileGateDwell(
            @RequestBody GateDwellIntervalDto dwell) {
        return ResponseEntity.ok(attendanceService.reconcileGateDwellSession(dwell));
    }

    @GetMapping("/attendance/history")
    public ResponseEntity<List<FlatAttendanceReconciliationDto>> getFlatHistory(
            @RequestParam Long communityId,
            @RequestParam Long flatId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(attendanceService.getFlatAttendanceHistory(communityId, flatId, startDate, endDate));
    }

    @PostMapping("/salary/settle")
    public ResponseEntity<MonthlySalarySettlementDto> settleMonthlySalary(
            @RequestParam Long contractId,
            @RequestParam String billingMonth) {
        return ResponseEntity.ok(attendanceService.calculateMonthlySalarySettlement(contractId, billingMonth));
    }
}
