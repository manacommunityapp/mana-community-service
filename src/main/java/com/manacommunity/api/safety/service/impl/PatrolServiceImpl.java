package com.manacommunity.api.safety.service.impl;

import com.manacommunity.api.safety.dto.PatrolCheckpointResponse;
import com.manacommunity.api.safety.dto.PatrolLogResponse;
import com.manacommunity.api.safety.dto.PatrolScanRequest;
import com.manacommunity.api.safety.model.GuardShift;
import com.manacommunity.api.safety.model.PatrolCheckpoint;
import com.manacommunity.api.safety.model.PatrolLog;
import com.manacommunity.api.safety.repository.GuardShiftRepository;
import com.manacommunity.api.safety.repository.PatrolCheckpointRepository;
import com.manacommunity.api.safety.repository.PatrolLogRepository;
import com.manacommunity.api.safety.service.PatrolService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatrolServiceImpl implements PatrolService {

    private final PatrolCheckpointRepository checkpointRepository;
    private final PatrolLogRepository patrolLogRepository;
    private final GuardShiftRepository shiftRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PatrolCheckpointResponse> getCheckpoints(Long communityId) {
        return checkpointRepository.findByCommunityIdAndActiveTrueOrderBySequenceOrderAsc(communityId)
                .stream()
                .map(this::toCheckpointResponse)
                .toList();
    }

    @Override
    @Transactional
    public PatrolLogResponse scanCheckpoint(PatrolScanRequest request, AppUser guard) {
        PatrolCheckpoint checkpoint = checkpointRepository.findById(request.getCheckpointId())
                .orElseThrow(() -> new RuntimeException("Checkpoint not found with id: " + request.getCheckpointId()));

        PatrolLog log = PatrolLog.builder()
                .guard(guard)
                .checkpoint(checkpoint)
                .scannedAt(LocalDateTime.now())
                .notes(request.getNotes())
                .community(checkpoint.getCommunity())
                .shiftId(request.getShiftId())
                .build();

        PatrolLog saved = patrolLogRepository.save(log);

        // Update shift checkpoint count if shiftId provided
        if (request.getShiftId() != null) {
            shiftRepository.findById(request.getShiftId()).ifPresent(shift -> {
                shift.setCheckpointsCompleted(shift.getCheckpointsCompleted() + 1);
                shiftRepository.save(shift);
            });
        }

        return toLogResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatrolLogResponse> getPatrolLogs(Long shiftId) {
        return patrolLogRepository.findByShiftIdOrderByScannedAtAsc(shiftId)
                .stream()
                .map(this::toLogResponse)
                .toList();
    }

    private PatrolCheckpointResponse toCheckpointResponse(PatrolCheckpoint checkpoint) {
        PatrolCheckpointResponse response = new PatrolCheckpointResponse();
        response.setId(checkpoint.getId());
        response.setName(checkpoint.getName());
        response.setLocation(checkpoint.getLocation());
        response.setQrCode(checkpoint.getQrCode());
        response.setSequenceOrder(checkpoint.getSequenceOrder());
        response.setActive(checkpoint.getActive());
        response.setCreatedAt(checkpoint.getCreatedAt());
        return response;
    }

    private PatrolLogResponse toLogResponse(PatrolLog log) {
        PatrolLogResponse response = new PatrolLogResponse();
        response.setId(log.getId());
        response.setGuardName(log.getGuard() != null ? log.getGuard().getFullName() : null);
        response.setCheckpointName(log.getCheckpoint() != null ? log.getCheckpoint().getName() : null);
        response.setCheckpointLocation(log.getCheckpoint() != null ? log.getCheckpoint().getLocation() : null);
        response.setScannedAt(log.getScannedAt());
        response.setNotes(log.getNotes());
        response.setShiftId(log.getShiftId());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
