package com.manacommunity.api.scheduler;

import com.manacommunity.api.model.karate.SportsKarateBelt;
import com.manacommunity.api.model.karate.SportsKarateEnrollment;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.karate.SportsKarateBeltRepository;
import com.manacommunity.api.repository.karate.SportsKarateEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Nightly job that checks whether any active enrolled student now meets the
 * criteria for their next belt (min classes attended + min months enrolled)
 * and sets gradingEligible=true on the enrollment so instructors can
 * schedule a grading exam for them.
 *
 * Runs daily at 02:00 AM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportsKarateGradingEligibilityScheduler {

    private final SportsKarateEnrollmentRepository enrollmentRepo;
    private final SportsKarateBeltRepository beltRepo;
    private final CommunityRepository communityRepo;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void checkGradingEligibility() {
        List<Long> communityIds = communityRepo.findAll()
                .stream().map(c -> c.getId()).toList();

        int flagged = 0;
        for (Long communityId : communityIds) {
            List<SportsKarateEnrollment> active = enrollmentRepo.findActiveForCommunity(communityId);
            for (SportsKarateEnrollment enrollment : active) {
                try {
                    flagged += checkEnrollment(enrollment) ? 1 : 0;
                } catch (Exception ex) {
                    log.error("Eligibility check failed for enrollment {}: {}", enrollment.getId(), ex.getMessage());
                }
            }
        }
        if (flagged > 0) {
            log.info("Karate grading eligibility: {} enrollment(s) newly flagged as eligible", flagged);
        }
    }

    private boolean checkEnrollment(SportsKarateEnrollment enrollment) {
        if (Boolean.TRUE.equals(enrollment.getGradingEligible())) {
            return false; // already eligible
        }

        Long communityId = enrollment.getCommunity().getId();
        Integer currentRank = enrollment.getCurrentBelt() != null ? enrollment.getCurrentBelt().getRank() : 0;

        Optional<SportsKarateBelt> nextBeltOpt = beltRepo
                .findFirstByCommunityIdAndRankGreaterThanAndActiveTrueOrderByRankAsc(communityId, currentRank);

        if (nextBeltOpt.isEmpty()) {
            return false; // already at highest belt
        }

        SportsKarateBelt nextBelt = nextBeltOpt.get();
        int attended = enrollment.getTotalClassesAttended();
        long monthsEnrolled = ChronoUnit.MONTHS.between(
                enrollment.getEnrolledAt().toLocalDate(), LocalDateTime.now().toLocalDate());

        boolean meetsClasses = attended >= nextBelt.getMinClassesRequired();
        boolean meetsMonths  = monthsEnrolled >= nextBelt.getMinMonthsRequired();

        if (meetsClasses && meetsMonths) {
            enrollment.setGradingEligible(true);
            enrollmentRepo.save(enrollment);
            log.info("Enrollment {} is now eligible for {} (rank {}): {} classes, {} months",
                    enrollment.getId(), nextBelt.getName(), nextBelt.getRank(), attended, monthsEnrolled);
            return true;
        }
        return false;
    }
}
