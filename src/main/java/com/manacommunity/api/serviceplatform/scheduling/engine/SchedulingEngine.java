package com.manacommunity.api.serviceplatform.scheduling.engine;

import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import com.manacommunity.api.serviceplatform.scheduling.dto.AvailableSlotDto;
import com.manacommunity.api.serviceplatform.scheduling.entity.ProviderSchedule;
import com.manacommunity.api.serviceplatform.scheduling.entity.ProviderTimeOff;
import com.manacommunity.api.serviceplatform.scheduling.repository.ProviderScheduleRepository;
import com.manacommunity.api.serviceplatform.scheduling.repository.ProviderTimeOffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulingEngine {

    private final ProviderScheduleRepository scheduleRepository;
    private final ProviderTimeOffRepository timeOffRepository;
    private final WorkOrderRepository workOrderRepository;

    @Transactional(readOnly = true)
    public List<AvailableSlotDto> getAvailableSlots(Long providerId, LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<ProviderSchedule> schedules = scheduleRepository.findByProviderIdAndDayOfWeekAndIsActiveTrue(providerId, dayOfWeek);
        List<AvailableSlotDto> slots = new ArrayList<>();

        if (schedules.isEmpty()) {
            return slots;
        }

        // Check if provider is on leave on this date
        List<ProviderTimeOff> timeOffs = timeOffRepository.findByProviderIdAndIsApprovedTrue(providerId);
        boolean isOff = timeOffs.stream().anyMatch(t ->
                !date.isBefore(t.getStartDate()) && !date.isAfter(t.getEndDate())
        );

        if (isOff) {
            return slots;
        }

        List<WorkOrder> existingOrders = workOrderRepository.findByProviderIdAndStatus(providerId, WorkOrderStatus.SCHEDULED);

        for (ProviderSchedule sched : schedules) {
            LocalTime current = sched.getStartTime();
            int duration = sched.getSlotDurationMinutes();
            int buffer = sched.getBufferTimeMinutes();

            while (current.plusMinutes(duration).isBefore(sched.getEndTime()) || current.plusMinutes(duration).equals(sched.getEndTime())) {
                LocalTime slotStart = current;
                LocalTime slotEnd = current.plusMinutes(duration);
                LocalDateTime slotStartDt = LocalDateTime.of(date, slotStart);
                LocalDateTime slotEndDt = LocalDateTime.of(date, slotEnd);

                long overlappingCount = existingOrders.stream().filter(wo -> {
                    if (wo.getScheduledStart() == null || wo.getScheduledEnd() == null) return false;
                    return (wo.getScheduledStart().isBefore(slotEndDt) && wo.getScheduledEnd().isAfter(slotStartDt));
                }).count();

                boolean available = overlappingCount < sched.getMaxParallelJobs();

                slots.add(AvailableSlotDto.builder()
                        .date(date)
                        .startTime(slotStart)
                        .endTime(slotEnd)
                        .isAvailable(available)
                        .reason(available ? "Available" : "Slot booked")
                        .build());

                current = current.plusMinutes(duration + buffer);
            }
        }

        return slots;
    }
}
