package com.manacommunity.api.mobility.service;

import com.manacommunity.api.mobility.model.MobilityDriver;
import com.manacommunity.api.mobility.repository.MobilityDriverRepository;
import com.manacommunity.api.mobility.service.MobilityDriverLocationCacheService.CachedDriverLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Periodically persists Redis-cached driver GPS coordinates back to PostgreSQL every 30s
 * so database state stays eventually consistent without incurring a DB write per ping.
 */
@Slf4j
@Component
public class MobilityDriverLocationSyncScheduler {

    private final MobilityDriverRepository driverRepo;
    private final MobilityDriverLocationCacheService locationCacheService;

    public MobilityDriverLocationSyncScheduler(
            MobilityDriverRepository driverRepo,
            @Autowired(required = false) MobilityDriverLocationCacheService locationCacheService) {
        this.driverRepo = driverRepo;
        this.locationCacheService = locationCacheService;
    }

    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void flushLocationsToDb() {
        if (locationCacheService == null) return;
        Map<Long, CachedDriverLocation> dirty = locationCacheService.drainDirtyLocations();
        if (dirty.isEmpty()) return;

        List<MobilityDriver> drivers = driverRepo.findAllById(dirty.keySet());
        for (MobilityDriver driver : drivers) {
            CachedDriverLocation loc = dirty.get(driver.getId());
            if (loc != null) {
                driver.setCurrentLat(loc.lat());
                driver.setCurrentLng(loc.lng());
                driver.setLocationUpdatedAt(loc.updatedAt());
            }
        }
        driverRepo.saveAll(drivers);
        log.debug("Flushed {} cached driver GPS locations to DB", drivers.size());
    }
}
