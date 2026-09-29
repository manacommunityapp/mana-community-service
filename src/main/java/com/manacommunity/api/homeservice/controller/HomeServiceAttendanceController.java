package com.manacommunity.api.homeservice.controller;

import com.manacommunity.api.homeservice.dto.HomeServiceAttendanceRequest;
import com.manacommunity.api.homeservice.dto.MonthlyBillCalculationResult;
import com.manacommunity.api.homeservice.model.entity.HomeServiceAttendanceEntity;
import com.manacommunity.api.homeservice.service.HomeServiceAttendanceAndBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController("homeServiceAttendanceController")
@RequestMapping("/api/v1/home-services/attendance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeServiceAttendanceController {
    private final HomeServiceAttendanceAndBillingService attendanceService;

    @PostMapping
    public ResponseEntity<HomeServiceAttendanceEntity> markAttendance(@RequestBody HomeServiceAttendanceRequest req) {
        return ResponseEntity.ok(attendanceService.markAttendance(req));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<HomeServiceAttendanceEntity>> getHistory(@PathVariable String bookingId) {
        return ResponseEntity.ok(attendanceService.getAttendanceHistory(bookingId));
    }

    @GetMapping("/booking/{bookingId}/bill")
    public ResponseEntity<MonthlyBillCalculationResult> getMonthlyBill(
            @PathVariable String bookingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(attendanceService.calculateMonthlyBill(bookingId, from, to));
    }
}
