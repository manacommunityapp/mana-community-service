package com.manacommunity.api.maintenance.controller;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.maintenance.entity.MaintenanceSchedule;
import com.manacommunity.api.maintenance.repository.MaintenanceScheduleRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// TODO: Add scheduler job to generate helpdesk tickets when next_due_date is reached.
@RestController
@RequestMapping("/api/maintenance/schedules")
@RequiredArgsConstructor
public class MaintenanceScheduleController {

    private final MaintenanceScheduleRepository repo;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<MaintenanceSchedule>> getSchedules(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(
                repo.findByCommunityIdAndIsActiveTrueOrderByNextDueDateAsc(caller.getCommunity().getId()));
    }

    @PostMapping
    public ResponseEntity<MaintenanceSchedule> createSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ScheduleRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        MaintenanceSchedule schedule = repo.save(MaintenanceSchedule.builder()
                .communityId(caller.getCommunity().getId())
                .title(request.getTitle())
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL")
                .recurrenceType(request.getRecurrenceType() != null ? request.getRecurrenceType() : "MONTHLY")
                .nextDueDate(request.getNextDueDate())
                .assignedVendorId(request.getAssignedVendorId())
                .notes(request.getNotes())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(schedule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceSchedule> updateSchedule(
            @PathVariable Long id,
            @RequestBody ScheduleRequest request) {
        MaintenanceSchedule schedule = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceSchedule", id));
        if (request.getTitle() != null) schedule.setTitle(request.getTitle());
        if (request.getCategory() != null) schedule.setCategory(request.getCategory());
        if (request.getRecurrenceType() != null) schedule.setRecurrenceType(request.getRecurrenceType());
        if (request.getNextDueDate() != null) schedule.setNextDueDate(request.getNextDueDate());
        if (request.getAssignedVendorId() != null) schedule.setAssignedVendorId(request.getAssignedVendorId());
        if (request.getNotes() != null) schedule.setNotes(request.getNotes());
        return ResponseEntity.ok(repo.save(schedule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        repo.findById(id).ifPresent(s -> {
            s.setIsActive(false);
            repo.save(s);
        });
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class ScheduleRequest {
        private String title;
        private String category;
        private String recurrenceType;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate nextDueDate;
        private Long assignedVendorId;
        private String notes;
    }
}
