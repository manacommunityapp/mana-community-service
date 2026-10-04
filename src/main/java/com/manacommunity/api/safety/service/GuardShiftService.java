package com.manacommunity.api.safety.service;

import com.manacommunity.api.safety.dto.GuardShiftRequest;
import com.manacommunity.api.safety.dto.GuardShiftResponse;
import com.manacommunity.api.safety.model.GuardShift;

import java.util.List;

public interface GuardShiftService {

    List<GuardShiftResponse> getShifts(Long communityId);

    GuardShiftResponse getShiftById(Long id);

    GuardShiftResponse createShift(GuardShiftRequest request, Long communityId);

    GuardShiftResponse updateShiftStatus(Long id, GuardShift.ShiftStatus status);
}
