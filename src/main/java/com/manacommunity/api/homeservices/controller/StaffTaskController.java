package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.model.StaffTask;
import com.manacommunity.api.homeservices.service.StaffTaskService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Staff Tasks")
@RestController
@RequestMapping("/api/staff/tasks")
@RequiredArgsConstructor
public class StaffTaskController {

    private final StaffTaskService service;

    @PostMapping
    public ResponseEntity<ApiResponse<StaffTask>> createTask(
            @RequestBody CreateTaskRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        StaffTask task = service.createTask(
                req.communityId, req.staffId, principal.getId(),
                req.title, req.description, req.dueDate);
        return ResponseEntity.ok(ApiResponse.ok(task));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<StaffTask>> updateStatus(
            @PathVariable Long taskId,
            @RequestParam Long communityId,
            @RequestParam StaffTask.TaskStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        StaffTask task = service.updateStatus(taskId, communityId, status);
        return ResponseEntity.ok(ApiResponse.ok(task));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<StaffTask>>> myTasks(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.getTasksForResident(principal.getId(), communityId)));
    }

    @Data
    public static class CreateTaskRequest {
        @NotNull Long communityId;
        @NotNull Long staffId;
        @NotBlank String title;
        String description;
        LocalDate dueDate;
    }
}
