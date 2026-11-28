package com.manacommunity.api.maintenance.scheduler;

import com.manacommunity.api.helpdesk.entity.HelpdeskTicket;
import com.manacommunity.api.helpdesk.repository.HelpdeskTicketRepository;
import com.manacommunity.api.maintenance.entity.MaintenanceSchedule;
import com.manacommunity.api.maintenance.repository.MaintenanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Automated scheduler that generates helpdesk maintenance tickets when asset schedules reach their next_due_date.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduleAutoTicketScheduler {

    private final MaintenanceScheduleRepository scheduleRepo;
    private final HelpdeskTicketRepository ticketRepo;

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public int generateDueTickets() {
        LocalDate today = LocalDate.now();
        List<MaintenanceSchedule> dueSchedules = scheduleRepo.findByIsActiveTrueAndNextDueDateLessThanEqual(today);

        log.info("[MaintenanceScheduler] Found {} due maintenance schedules on {}", dueSchedules.size(), today);
        int generatedCount = 0;

        for (MaintenanceSchedule schedule : dueSchedules) {
            try {
                // 1. Create Helpdesk Preventive Maintenance Ticket
                HelpdeskTicket ticket = HelpdeskTicket.builder()
                        .communityId(schedule.getCommunityId())
                        .residentId(0L) // 0L represents system facility administration
                        .flatNo("COMMON-FACILITY")
                        .tower("ESTATE")
                        .title("Scheduled Maintenance: " + schedule.getTitle())
                        .description("Automated preventive maintenance ticket generated from schedule #" + schedule.getId()
                                + (schedule.getNotes() != null ? "\nNotes: " + schedule.getNotes() : ""))
                        .category(schedule.getCategory() != null ? schedule.getCategory() : "PREVENTIVE_MAINTENANCE")
                        .priority("MEDIUM")
                        .status("OPEN")
                        .assignedTechnicianId(schedule.getAssignedVendorId())
                        .build();

                ticketRepo.save(ticket);
                generatedCount++;

                // 2. Advance nextDueDate based on recurrenceType
                LocalDate nextDate = advanceDueDate(schedule.getNextDueDate(), schedule.getRecurrenceType());
                schedule.setNextDueDate(nextDate);
                scheduleRepo.save(schedule);

                log.info("[MaintenanceScheduler] Created ticket #{} for schedule '{}', next due advanced to {}",
                        ticket.getId(), schedule.getTitle(), nextDate);
            } catch (Exception e) {
                log.error("[MaintenanceScheduler] Failed to generate ticket for schedule {}: {}", schedule.getId(), e.getMessage(), e);
            }
        }

        return generatedCount;
    }

    private LocalDate advanceDueDate(LocalDate currentDate, String recurrenceType) {
        if (currentDate == null) {
            currentDate = LocalDate.now();
        }
        if (recurrenceType == null) {
            return currentDate.plusMonths(1);
        }

        return switch (recurrenceType.toUpperCase()) {
            case "DAILY" -> currentDate.plusDays(1);
            case "WEEKLY" -> currentDate.plusWeeks(1);
            case "BIWEEKLY", "FORTNIGHTLY" -> currentDate.plusWeeks(2);
            case "MONTHLY" -> currentDate.plusMonths(1);
            case "QUARTERLY" -> currentDate.plusMonths(3);
            case "BIANNUALLY", "HALF_YEARLY" -> currentDate.plusMonths(6);
            case "ANNUALLY", "YEARLY" -> currentDate.plusYears(1);
            default -> currentDate.plusMonths(1);
        };
    }
}
