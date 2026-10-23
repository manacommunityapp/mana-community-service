package com.manacommunity.api.safety.service.impl;

import com.manacommunity.api.safety.dto.GuardShiftRequest;
import com.manacommunity.api.safety.dto.GuardShiftResponse;
import com.manacommunity.api.safety.model.GuardShift;
import com.manacommunity.api.safety.repository.GuardShiftRepository;
import com.manacommunity.api.safety.service.GuardShiftService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuardShiftServiceImpl implements GuardShiftService {

    private final GuardShiftRepository shiftRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GuardShiftResponse> getShifts(Long communityId) {
        return shiftRepository.findByCommunityIdOrderByStartTimeDesc(communityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GuardShiftResponse getShiftById(Long id) {
        GuardShift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guard shift not found with id: " + id));
        return toResponse(shift);
    }

    @Override
    @Transactional
    public GuardShiftResponse createShift(GuardShiftRequest request, Long communityId) {
        AppUser guard = new AppUser();
        guard.setId(request.getGuardId());

        Community community = new Community();
        community.setId(communityId);

        GuardShift shift = GuardShift.builder()
                .guard(guard)
                .community(community)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .area(request.getArea())
                .totalCheckpoints(request.getTotalCheckpoints() != null ? request.getTotalCheckpoints() : 0)
                .build();

        return toResponse(shiftRepository.save(shift));
    }

    @Override
    @Transactional
    public GuardShiftResponse updateShiftStatus(Long id, GuardShift.ShiftStatus status) {
        GuardShift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guard shift not found with id: " + id));

        shift.setStatus(status);
        return toResponse(shiftRepository.save(shift));
    }

    private GuardShiftResponse toResponse(GuardShift shift) {
        GuardShiftResponse response = new GuardShiftResponse();
        response.setId(shift.getId());
        response.setGuardId(shift.getGuard().getId());
        response.setGuardName(shift.getGuard() != null ? shift.getGuard().getFullName() : null);
        response.setStartTime(shift.getStartTime());
        response.setEndTime(shift.getEndTime());
        response.setArea(shift.getArea());
        response.setStatus(shift.getStatus().name());
        response.setCheckpointsCompleted(shift.getCheckpointsCompleted());
        response.setTotalCheckpoints(shift.getTotalCheckpoints());
        response.setCreatedAt(shift.getCreatedAt());
        return response;
    }
}
