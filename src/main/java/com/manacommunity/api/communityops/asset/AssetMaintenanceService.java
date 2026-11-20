package com.manacommunity.api.communityops.asset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetMaintenanceService {

    private final CommunityAssetRepository assetRepo;
    private final AssetMaintenanceScheduleRepository scheduleRepo;
    private final AssetMaintenanceLogRepository logRepo;

    @Transactional
    public CommunityAsset createAsset(Long communityId, AssetDto req) {
        return assetRepo.save(CommunityAsset.builder()
            .communityId(communityId)
            .name(req.name())
            .category(req.category())
            .modelNumber(req.modelNumber())
            .serialNumber(req.serialNumber())
            .location(req.location())
            .purchaseDate(req.purchaseDate())
            .warrantyExpiry(req.warrantyExpiry())
            .amcVendor(req.amcVendor())
            .amcExpiry(req.amcExpiry())
            .status(AssetStatus.OPERATIONAL)
            .notes(req.notes())
            .build());
    }

    @Transactional
    public CommunityAsset updateAsset(Long id, AssetDto req) {
        CommunityAsset asset = assetRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Asset not found"));
        asset.setName(req.name());
        asset.setCategory(req.category());
        asset.setModelNumber(req.modelNumber());
        asset.setSerialNumber(req.serialNumber());
        asset.setLocation(req.location());
        asset.setPurchaseDate(req.purchaseDate());
        asset.setWarrantyExpiry(req.warrantyExpiry());
        asset.setAmcVendor(req.amcVendor());
        asset.setAmcExpiry(req.amcExpiry());
        asset.setNotes(req.notes());
        asset.setUpdatedAt(Instant.now());
        return assetRepo.save(asset);
    }

    @Transactional
    public AssetMaintenanceLog logMaintenance(Long assetId, MaintenanceLogDto req, Long reportedBy) {
        AssetMaintenanceLog log2 = logRepo.save(AssetMaintenanceLog.builder()
            .assetId(assetId)
            .scheduleId(req.scheduleId())
            .taskDescription(req.taskDescription())
            .status(req.status() != null ? req.status() : MaintenanceLogStatus.COMPLETED)
            .scheduledDate(req.scheduledDate())
            .completedDate(req.completedDate())
            .performedBy(req.performedBy())
            .costActual(req.costActual())
            .notes(req.notes())
            .reportedBy(reportedBy)
            .build());

        // Advance schedule's last_done and next_due
        if (req.scheduleId() != null && req.completedDate() != null) {
            scheduleRepo.findById(req.scheduleId()).ifPresent(schedule -> {
                schedule.setLastDone(req.completedDate());
                schedule.setNextDue(computeNextDue(req.completedDate(), schedule.getFrequency()));
                scheduleRepo.save(schedule);
            });
        }
        return log2;
    }

    private LocalDate computeNextDue(LocalDate from, MaintenanceFrequency freq) {
        return switch (freq) {
            case WEEKLY -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
            case QUARTERLY -> from.plusMonths(3);
            case HALF_YEARLY -> from.plusMonths(6);
            case YEARLY -> from.plusYears(1);
        };
    }

    public List<CommunityAsset> getAssets(Long communityId, AssetCategory category, AssetStatus status) {
        if (category != null) return assetRepo.findByCommunityIdAndCategory(communityId, category);
        if (status != null) return assetRepo.findByCommunityIdAndStatus(communityId, status);
        return assetRepo.findByCommunityId(communityId);
    }

    public List<AssetMaintenanceLog> getHistory(Long assetId) {
        return logRepo.findByAssetIdOrderByScheduledDateDesc(assetId);
    }

    public List<AssetMaintenanceSchedule> getOverdue(Long communityId) {
        return scheduleRepo.findOverdueForCommunity(communityId, LocalDate.now());
    }

    public List<AssetMaintenanceSchedule> getUpcoming(Long communityId, int days) {
        return scheduleRepo.findUpcomingForCommunity(communityId, LocalDate.now(), LocalDate.now().plusDays(days));
    }

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void markOverdueLogs() {
        LocalDate today = LocalDate.now();
        List<AssetMaintenanceLog> scheduled = logRepo.findByStatus(MaintenanceLogStatus.SCHEDULED);
        for (AssetMaintenanceLog l : scheduled) {
            if (l.getScheduledDate().isBefore(today)) {
                l.setStatus(MaintenanceLogStatus.OVERDUE);
                logRepo.save(l);
            }
        }
        log.info("[AssetMaint] marked {} logs as OVERDUE", scheduled.size());
    }
}
