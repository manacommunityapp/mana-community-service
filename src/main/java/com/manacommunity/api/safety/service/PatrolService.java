package com.manacommunity.api.safety.service;

import com.manacommunity.api.safety.dto.PatrolCheckpointResponse;
import com.manacommunity.api.safety.dto.PatrolLogResponse;
import com.manacommunity.api.safety.dto.PatrolScanRequest;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface PatrolService {

    List<PatrolCheckpointResponse> getCheckpoints(Long communityId);

    PatrolLogResponse scanCheckpoint(PatrolScanRequest request, AppUser guard);

    List<PatrolLogResponse> getPatrolLogs(Long shiftId);
}
