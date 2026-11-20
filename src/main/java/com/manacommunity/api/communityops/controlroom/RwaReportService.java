package com.manacommunity.api.communityops.controlroom;

import com.manacommunity.api.communityops.asset.AssetMaintenanceLogRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RwaReportService {

    private final StaffShiftRepository shiftRepo;
    private final ParcelLogRepository parcelRepo;
    private final DigitalPassRepository passRepo;
    private final AssetMaintenanceLogRepository maintLogRepo;
    private final MoveRequestRepository moveRepo;

    public RwaMonthlyReport generateMonthlyReport(Long communityId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.plusMonths(1);
        Instant fromI = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toI = to.atStartOfDay(ZoneOffset.UTC).toInstant();

        var shifts = shiftRepo.findByCommunityIdAndShiftDateBetween(communityId, from, to.minusDays(1));
        Map<String, Long> byStatus = shifts.stream()
            .collect(Collectors.groupingBy(s -> s.getStatus().name(), Collectors.counting()));

        return new RwaMonthlyReport(
            year, month,
            shifts.size(),
            byStatus.getOrDefault(AttendanceStatus.PRESENT.name(), 0L),
            byStatus.getOrDefault(AttendanceStatus.ABSENT.name(), 0L),
            parcelRepo.countByCommunityIdAndStatus(communityId, ParcelStatus.RECEIVED),
            parcelRepo.countByCommunityIdAndStatus(communityId, ParcelStatus.PICKED_UP),
            moveRepo.countByCommunityIdAndStatus(communityId, MoveStatus.COMPLETED),
            moveRepo.countByCommunityIdAndStatus(communityId, MoveStatus.COMPLETED),
            passRepo.countByCommunityIdAndValidFromBetween(communityId, fromI, toI),
            maintLogRepo.countByCommunityIdAndStatus(communityId, MaintenanceLogStatus.COMPLETED),
            0L, // cost aggregation reserved for future
            byStatus
        );
    }
}
