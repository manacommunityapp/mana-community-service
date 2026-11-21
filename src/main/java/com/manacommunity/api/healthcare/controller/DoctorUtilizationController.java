package com.manacommunity.api.healthcare.controller;

import com.manacommunity.api.healthcare.entity.DoctorSlotUtilization;
import com.manacommunity.api.healthcare.service.DoctorUtilizationService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/healthcare/utilization")
@RequiredArgsConstructor
public class DoctorUtilizationController {

    private final DoctorUtilizationService utilizationService;

    @PostMapping
    public ResponseEntity<DoctorSlotUtilization> upsert(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(utilizationService.upsert(
            principal.getCommunityId(),
            Long.parseLong(body.get("doctorId").toString()),
            LocalDate.parse(body.get("slotDate").toString()),
            Integer.parseInt(body.get("totalSlots").toString()),
            Integer.parseInt(body.get("bookedSlots").toString()),
            Integer.parseInt(body.get("attendedSlots").toString()),
            Integer.parseInt(body.get("noShowSlots").toString()),
            Integer.parseInt(body.get("cancelledSlots").toString())
        ));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<DoctorSlotUtilization>> forDoctor(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(utilizationService.getForDoctor(principal.getCommunityId(), doctorId, from, to));
    }

    @GetMapping("/date")
    public ResponseEntity<List<DoctorSlotUtilization>> forDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(utilizationService.getForDate(principal.getCommunityId(), date));
    }
}
