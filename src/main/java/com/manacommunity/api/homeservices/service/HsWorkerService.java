package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.entity.*;
import com.manacommunity.api.homeservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HsWorkerService {

    private final HsWorkerRepository workerRepo;
    private final HsWorkerSkillRepository skillRepo;
    private final HsWorkerAvailabilityRepository availRepo;
    private final HsBookingRatingRepository ratingRepo;

    public List<HsWorker> listActive(Long communityId) {
        return workerRepo.findByCommunityIdAndIsActiveTrue(communityId);
    }

    public List<HsWorker> findByCategory(Long communityId, String category) {
        List<Long> workerIds = skillRepo.findByCategory(category).stream()
            .map(HsWorkerSkill::getWorkerId).toList();
        return workerRepo.findByCommunityIdAndIsActiveTrue(communityId).stream()
            .filter(w -> workerIds.contains(w.getId())).toList();
    }

    @Transactional
    public HsWorkerAvailability addAvailability(HsWorkerAvailability slot) {
        return availRepo.save(slot);
    }

    public List<HsWorkerAvailability> availableSlots(Long communityId, LocalDate date) {
        return availRepo.findByCommunityIdAndAvailableDateAndIsBookedFalse(communityId, date);
    }

    @Transactional
    public void refreshWorkerRating(Long workerId) {
        Double avg = ratingRepo.avgRatingByWorkerId(workerId);
        if (avg == null) return;
        workerRepo.findById(workerId).ifPresent(w -> {
            w.setRating(java.math.BigDecimal.valueOf(avg).setScale(2, java.math.RoundingMode.HALF_UP));
            workerRepo.save(w);
        });
    }
}
