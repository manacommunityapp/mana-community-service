package com.manacommunity.api.maintenance.service;

import com.manacommunity.api.maintenance.entity.MaintenanceSchedule;
import com.manacommunity.api.maintenance.repository.MaintenanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduleJobService {

    private final MaintenanceScheduleRepository maintenanceScheduleRepository;

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void processDueSchedules() {
        LocalDate today = LocalDate.now();
        List<MaintenanceSchedule> due = maintenanceScheduleRepository.findByIsActiveTrueAndNextDueDateLessThanEqual(today);
        for (MaintenanceSchedule schedule : due) {
            schedule.setLastRunDate(today);
            schedule.setNextDueDate(computeNext(schedule.getRecurrenceType(), schedule.getNextDueDate()));
            maintenanceScheduleRepository.save(schedule);
        }
        if (!due.isEmpty()) {
            log.info("MaintenanceScheduleJob: advanced {} due schedules", due.size());
        }
    }

    private LocalDate computeNext(String recurrenceType, LocalDate from) {
        return switch (recurrenceType) {
            case "DAILY"    -> from.plusDays(1);
            case "WEEKLY"   -> from.plusWeeks(1);
            case "MONTHLY"  -> from.plusMonths(1);
            case "ANNUALLY" -> from.plusYears(1);
            default         -> from.plusMonths(1);
        };
    }
}
