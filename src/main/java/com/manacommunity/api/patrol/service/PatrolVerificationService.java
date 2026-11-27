package com.manacommunity.api.patrol.service;

import com.manacommunity.api.patrol.dto.PatrolDtos.*;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface PatrolVerificationService {

    PatrolScanResult verifyAndRecordScan(PatrolScanSubmission submission, AppUser guard);

    List<PatrolBreachReport> checkAndEscalateMissedCheckpoints(Long communityId, String routeName, Long shiftId);
}
