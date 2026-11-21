package com.manacommunity.api.communityops.controlroom;

import com.manacommunity.api.communityops.asset.AssetMaintenanceLogRepository;
import com.manacommunity.api.communityops.asset.AssetMaintenanceScheduleRepository;
import com.manacommunity.api.communityops.asset.MaintenanceLogStatus;
import com.manacommunity.api.communityops.attendance.AttendanceStatus;
import com.manacommunity.api.communityops.attendance.StaffShiftRepository;
import com.manacommunity.api.communityops.move.MoveRequestRepository;
import com.manacommunity.api.communityops.move.MoveStatus;
import com.manacommunity.api.communityops.parcel.ParcelLogRepository;
import com.manacommunity.api.communityops.parcel.ParcelStatus;
import com.manacommunity.api.communityops.pass.DigitalPassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;

@Service
@RequiredArgsConstructor
public class OpsControlRoomService {

    private final StaffShiftRepository shiftRepo;
    private final ParcelLogRepository parcelRepo;
    private final DigitalPassRepository passRepo;
    private final AssetMaintenanceLogRepository maintLogRepo;
    private final AssetMaintenanceScheduleRepository maintScheduleRepo;
    private final MoveRequestRepository moveRepo;

    public OpsControlRoomSummary getSummary(Long communityId) {
        LocalDate today = LocalDate.now();
        Instant dayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return new OpsControlRoomSummary(
            shiftRepo.countByCommunityIdAndShiftDateAndStatus(communityId, today, AttendanceStatus.PRESENT),
            parcelRepo.countByCommunityIdAndStatus(communityId, ParcelStatus.RECEIVED),
            passRepo.countByCommunityIdAndValidFromBetween(communityId, dayStart, dayEnd),
            maintLogRepo.countByCommunityIdAndStatus(communityId, MaintenanceLogStatus.OVERDUE),
            maintScheduleRepo.countByAssetCommunityIdAndNextDueBetween(communityId, today, today.plusDays(7)),
            moveRepo.countByCommunityIdAndStatus(communityId, MoveStatus.REQUESTED)
        );
    }
}
