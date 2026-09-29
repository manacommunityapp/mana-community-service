package com.manacommunity.api.homeservice.controller;

import com.manacommunity.api.homeservice.model.entity.*;
import com.manacommunity.api.homeservice.model.enums.HomeServiceVerificationStatus;
import com.manacommunity.api.homeservice.service.HomeServiceWorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController("homeServiceWorkerController")
@RequestMapping("/api/v1/home-services/workers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeServiceWorkerController {
    private final HomeServiceWorkerService workerService;

    @GetMapping
    public ResponseEntity<List<HomeServiceWorkerEntity>> getWorkers(
            @RequestParam(required = false, defaultValue = "comm-mana-1") String communityId,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) HomeServiceVerificationStatus status) {
        return ResponseEntity.ok(workerService.getWorkers(communityId, minRating, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HomeServiceWorkerEntity> getWorkerById(@PathVariable String id) {
        return ResponseEntity.ok(workerService.getWorkerById(id));
    }

    @GetMapping("/{id}/skills")
    public ResponseEntity<List<WorkerServiceSkillEntity>> getSkills(@PathVariable String id) {
        return ResponseEntity.ok(workerService.getWorkerSkills(id));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<WorkerAvailabilitySlotEntity>> getSlots(@PathVariable String id) {
        return ResponseEntity.ok(workerService.getWorkerSlots(id));
    }

    @GetMapping("/{id}/flats")
    public ResponseEntity<List<WorkerFlatAssignmentEntity>> getFlats(@PathVariable String id) {
        return ResponseEntity.ok(workerService.getWorkerFlats(id));
    }

    @PostMapping
    public ResponseEntity<HomeServiceWorkerEntity> registerWorker(@RequestBody HomeServiceWorkerEntity worker) {
        return ResponseEntity.ok(workerService.registerWorker(worker));
    }
}
