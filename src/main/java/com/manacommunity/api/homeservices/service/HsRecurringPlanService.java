package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.entity.HsBooking;
import com.manacommunity.api.homeservices.entity.HsRecurringPlan;
import com.manacommunity.api.homeservices.repository.HsRecurringPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HsRecurringPlanService {

    private final HsRecurringPlanRepository planRepo;
    private final HsBookingService bookingService;

    public List<HsRecurringPlan> myPlans(Long userId) {
        return planRepo.findByUserId(userId);
    }

    @Transactional
    public HsRecurringPlan create(HsRecurringPlan plan) {
        return planRepo.save(plan);
    }

    @Transactional
    public HsRecurringPlan pause(Long planId) {
        HsRecurringPlan p = planRepo.findById(planId).orElseThrow();
        p.setStatus("PAUSED");
        p.setUpdatedAt(Instant.now());
        return planRepo.save(p);
    }

    @Transactional
    public HsRecurringPlan cancel(Long planId) {
        HsRecurringPlan p = planRepo.findById(planId).orElseThrow();
        p.setStatus("CANCELLED");
        p.setUpdatedAt(Instant.now());
        return planRepo.save(p);
    }

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void generateDueBookings() {
        List<HsRecurringPlan> due = planRepo.findByStatusAndNextServiceDateLessThanEqual("ACTIVE", LocalDate.now());
        for (HsRecurringPlan plan : due) {
            try {
                HsBooking booking = HsBooking.builder()
                    .communityId(plan.getCommunityId()).userId(plan.getUserId())
                    .packageId(plan.getPackageId()).category(plan.getCategory())
                    .bookingType("SCHEDULED")
                    .scheduledAt(plan.getNextServiceDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC))
                    .status("PENDING").build();
                bookingService.create(booking);
                plan.setNextServiceDate(advanceDate(plan.getNextServiceDate(), plan.getFrequency()));
                plan.setUpdatedAt(Instant.now());
                planRepo.save(plan);
            } catch (Exception e) {
                log.warn("Failed to generate recurring booking for plan {}: {}", plan.getId(), e.getMessage());
            }
        }
    }

    private LocalDate advanceDate(LocalDate from, String frequency) {
        return switch (frequency) {
            case "WEEKLY"      -> from.plusWeeks(1);
            case "FORTNIGHTLY" -> from.plusWeeks(2);
            case "MONTHLY"     -> from.plusMonths(1);
            case "QUARTERLY"   -> from.plusMonths(3);
            default            -> from.plusMonths(1);
        };
    }
}
