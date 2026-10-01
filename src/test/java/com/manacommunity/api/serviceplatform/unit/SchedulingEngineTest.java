package com.manacommunity.api.serviceplatform.unit;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import com.manacommunity.api.serviceplatform.scheduling.dto.AvailableSlotDto;
import com.manacommunity.api.serviceplatform.scheduling.engine.SchedulingEngine;
import com.manacommunity.api.serviceplatform.scheduling.entity.ProviderSchedule;
import com.manacommunity.api.serviceplatform.scheduling.entity.ProviderTimeOff;
import com.manacommunity.api.serviceplatform.scheduling.repository.ProviderScheduleRepository;
import com.manacommunity.api.serviceplatform.scheduling.repository.ProviderTimeOffRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Scheduling Engine Unit Tests")
class SchedulingEngineTest {

    @Mock
    private ProviderScheduleRepository scheduleRepository;

    @Mock
    private ProviderTimeOffRepository timeOffRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @InjectMocks
    private SchedulingEngine engine;

    @Test
    @DisplayName("Should generate available slots for normal working schedule")
    void testGenerateAvailableSlotsNormal() {
        LocalDate date = LocalDate.of(2026, 10, 5); // Monday = 1
        ProviderSchedule schedule = ProviderSchedule.builder()
                .dayOfWeek(1)
                .isActive(true)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(60)
                .bufferTimeMinutes(0)
                .maxParallelJobs(1)
                .build();

        when(scheduleRepository.findByProviderIdAndDayOfWeekAndIsActiveTrue(eq(1L), eq(1)))
                .thenReturn(List.of(schedule));
        when(timeOffRepository.findByProviderIdAndIsApprovedTrue(1L))
                .thenReturn(Collections.emptyList());
        when(workOrderRepository.findByProviderIdAndStatus(1L, WorkOrderStatus.SCHEDULED))
                .thenReturn(Collections.emptyList());

        List<AvailableSlotDto> slots = engine.getAvailableSlots(1L, date);

        assertNotNull(slots);
        assertEquals(3, slots.size()); // 9-10, 10-11, 11-12
        assertEquals(LocalTime.of(9, 0), slots.get(0).getStartTime());
        assertEquals(LocalTime.of(10, 0), slots.get(0).getEndTime());
        assertTrue(slots.get(0).getIsAvailable());
    }

    @Test
    @DisplayName("Should return empty list for non-working day")
    void testGenerateAvailableSlotsNonWorkingDay() {
        LocalDate date = LocalDate.of(2026, 10, 4); // Sunday = 7
        when(scheduleRepository.findByProviderIdAndDayOfWeekAndIsActiveTrue(eq(1L), eq(7)))
                .thenReturn(Collections.emptyList());

        List<AvailableSlotDto> slots = engine.getAvailableSlots(1L, date);

        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Should block slots if provider has approved time-off")
    void testGenerateAvailableSlotsWithTimeOff() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        ProviderSchedule schedule = ProviderSchedule.builder()
                .dayOfWeek(1)
                .isActive(true)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(60)
                .bufferTimeMinutes(0)
                .maxParallelJobs(1)
                .build();

        ProviderTimeOff timeOff = ProviderTimeOff.builder()
                .startDate(date)
                .endDate(date)
                .isApproved(true)
                .build();

        when(scheduleRepository.findByProviderIdAndDayOfWeekAndIsActiveTrue(eq(1L), eq(1)))
                .thenReturn(List.of(schedule));
        when(timeOffRepository.findByProviderIdAndIsApprovedTrue(1L))
                .thenReturn(List.of(timeOff));

        List<AvailableSlotDto> slots = engine.getAvailableSlots(1L, date);

        assertTrue(slots.isEmpty());
    }
}
