package com.manacommunity.api.serviceplatform.scheduling.controller;

import com.manacommunity.api.serviceplatform.scheduling.dto.AvailableSlotDto;
import com.manacommunity.api.serviceplatform.scheduling.dto.ProviderScheduleDto;
import com.manacommunity.api.serviceplatform.scheduling.entity.ProviderTimeOff;
import com.manacommunity.api.serviceplatform.scheduling.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/service-platform/scheduling")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    @GetMapping("/slots")
    public ResponseEntity<List<AvailableSlotDto>> getSlots(
            @RequestParam Long providerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(schedulingService.getSlots(providerId, date));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ProviderScheduleDto>> getSchedules(@PathVariable Long providerId) {
        return ResponseEntity.ok(schedulingService.getProviderSchedules(providerId));
    }

    @PostMapping("/schedule")
    public ResponseEntity<ProviderScheduleDto> saveSchedule(@RequestBody ProviderScheduleDto dto) {
        return ResponseEntity.ok(schedulingService.setSchedule(dto));
    }

    @PostMapping("/time-off")
    public ResponseEntity<ProviderTimeOff> recordTimeOff(@RequestBody ProviderTimeOff timeOff) {
        return ResponseEntity.ok(schedulingService.recordTimeOff(timeOff));
    }
}
