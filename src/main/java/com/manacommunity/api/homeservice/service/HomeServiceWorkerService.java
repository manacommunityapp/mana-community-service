package com.manacommunity.api.homeservice.service;

import com.manacommunity.api.homeservice.model.entity.*;
import com.manacommunity.api.homeservice.model.enums.HomeServiceVerificationStatus;
import com.manacommunity.api.homeservice.repository.*;
import com.manacommunity.api.homeservice.exception.HomeServiceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service("homeServiceWorkerService")
@RequiredArgsConstructor
public class HomeServiceWorkerService {
    private final HomeServiceWorkerRepository workerRepository;
    private final WorkerServiceSkillRepository skillRepository;
    private final WorkerAvailabilitySlotRepository slotRepository;
    private final WorkerFlatAssignmentRepository assignmentRepository;

    public List<HomeServiceWorkerEntity> getWorkers(String communityId, BigDecimal minRating, HomeServiceVerificationStatus status) {
        return workerRepository.searchWorkers(communityId, minRating, status);
    }

    public HomeServiceWorkerEntity getWorkerById(String workerId) {
        return workerRepository.findById(workerId)
                .orElseThrow(() -> new HomeServiceNotFoundException("Worker not found: " + workerId));
    }

    public List<WorkerServiceSkillEntity> getWorkerSkills(String workerId) {
        return skillRepository.findByWorkerId(workerId);
    }

    public List<WorkerAvailabilitySlotEntity> getWorkerSlots(String workerId) {
        return slotRepository.findByWorkerId(workerId);
    }

    public List<WorkerFlatAssignmentEntity> getWorkerFlats(String workerId) {
        return assignmentRepository.findByWorkerIdAndActiveTrue(workerId);
    }

    @Transactional
    public HomeServiceWorkerEntity registerWorker(HomeServiceWorkerEntity worker) {
        if (worker.getId() == null) {
            worker.setId(UUID.randomUUID().toString());
        }
        worker.setCreatedAt(LocalDateTime.now());
        worker.setUpdatedAt(LocalDateTime.now());
        return workerRepository.save(worker);
    }

    @Transactional
    public HomeServiceWorkerEntity updateVerification(String workerId, HomeServiceVerificationStatus status, boolean policeVerified, boolean communityVerified) {
        HomeServiceWorkerEntity worker = getWorkerById(workerId);
        worker.setVerificationStatus(status);
        worker.setPoliceVerified(policeVerified);
        worker.setCommunityVerified(communityVerified);
        worker.setSecurityVerified(true);
        worker.setUpdatedAt(LocalDateTime.now());
        return workerRepository.save(worker);
    }
}
