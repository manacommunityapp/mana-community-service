package com.manacommunity.api.serviceplatform.scheduling.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.scheduling.dto.AvailableSlotDto;
import com.manacommunity.api.serviceplatform.scheduling.dto.ProviderScheduleDto;
import com.manacommunity.api.serviceplatform.scheduling.engine.SchedulingEngine;
import com.manacommunity.api.serviceplatform.scheduling.entity.ProviderSchedule;
import com.manacommunity.api.serviceplatform.scheduling.entity.ProviderTimeOff;
import com.manacommunity.api.serviceplatform.scheduling.repository.ProviderScheduleRepository;
import com.manacommunity.api.serviceplatform.scheduling.repository.ProviderTimeOffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulingService {

    private final SchedulingEngine schedulingEngine;
    private final ProviderScheduleRepository scheduleRepository;
    private final ProviderTimeOffRepository timeOffRepository;
    private final ServiceProviderRepository providerRepository;

    @Transactional(readOnly = true)
    public List<AvailableSlotDto> getSlots(Long providerId, LocalDate date) {
        return schedulingEngine.getAvailableSlots(providerId, date);
    }

    @Transactional
    public ProviderScheduleDto setSchedule(ProviderScheduleDto dto) {
        ServiceProvider provider = providerRepository.findById(dto.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", dto.getProviderId()));

        ProviderSchedule schedule = ProviderSchedule.builder()
                .id(dto.getId())
                .provider(provider)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .slotDurationMinutes(dto.getSlotDurationMinutes() != null ? dto.getSlotDurationMinutes() : 60)
                .bufferTimeMinutes(dto.getBufferTimeMinutes() != null ? dto.getBufferTimeMinutes() : 15)
                .maxParallelJobs(dto.getMaxParallelJobs() != null ? dto.getMaxParallelJobs() : 1)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        schedule = scheduleRepository.save(schedule);
        return toDto(schedule);
    }

    @Transactional(readOnly = true)
    public List<ProviderScheduleDto> getProviderSchedules(Long providerId) {
        return scheduleRepository.findByProviderIdAndIsActiveTrue(providerId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ProviderTimeOff recordTimeOff(ProviderTimeOff timeOff) {
        return timeOffRepository.save(timeOff);
    }

    private ProviderScheduleDto toDto(ProviderSchedule s) {
        return ProviderScheduleDto.builder()
                .id(s.getId())
                .providerId(s.getProvider().getId())
                .dayOfWeek(s.getDayOfWeek())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .slotDurationMinutes(s.getSlotDurationMinutes())
                .bufferTimeMinutes(s.getBufferTimeMinutes())
                .maxParallelJobs(s.getMaxParallelJobs())
                .isActive(s.getIsActive())
                .build();
    }
}
