package com.manacommunity.api.scheduler;

import com.manacommunity.api.model.CommuteRide;
import com.manacommunity.api.model.CommuteRideStatus;
import com.manacommunity.api.repository.CommuteRideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommuteRideScheduler {

    private final CommuteRideRepository rideRepository;

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void expireCompletedRides() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        List<CommuteRide> expired = rideRepository.findExpiredActiveRides(cutoff);
        if (expired.isEmpty()) return;

        expired.forEach(ride -> ride.setStatus(CommuteRideStatus.COMPLETED));
        rideRepository.saveAll(expired);
        log.info("Auto-completed {} expired commute rides", expired.size());
    }
}
