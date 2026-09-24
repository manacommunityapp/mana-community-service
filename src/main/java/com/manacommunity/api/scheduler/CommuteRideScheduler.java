package com.manacommunity.api.scheduler;

import com.manacommunity.api.model.CommuteRide;
import com.manacommunity.api.model.CommuteRideStatus;
import com.manacommunity.api.repository.CommuteRideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommuteRideScheduler {

    private final CommuteRideRepository rideRepository;

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void expireCompletedRides() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        int totalCompleted = 0;
        Page<CommuteRide> page;
        int pageNum = 0;

        do {
            page = rideRepository.findExpiredRides(cutoff, PageRequest.of(pageNum, 100));
            for (CommuteRide ride : page.getContent()) {
                try {
                    ride.setStatus(CommuteRideStatus.COMPLETED);
                    rideRepository.save(ride);
                    totalCompleted++;
                } catch (Exception e) {
                    log.error("Failed to auto-complete ride {}: {}", ride.getId(), e.getMessage());
                }
            }
            pageNum++;
        } while (page.hasNext());

        if (totalCompleted > 0) {
            log.info("Auto-completed {} expired commute rides", totalCompleted);
        }
    }
}
