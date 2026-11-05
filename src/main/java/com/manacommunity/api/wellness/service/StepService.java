package com.manacommunity.api.wellness.service;

import com.manacommunity.api.wellness.dto.StepLogDto;
import com.manacommunity.api.wellness.entity.StepLog;
import com.manacommunity.api.wellness.repository.ActivityGoalRepository;
import com.manacommunity.api.wellness.repository.StepLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StepService {

    private final StepLogRepository stepLogRepository;
    private final ActivityGoalRepository activityGoalRepository;
    private final FitnessGoalService fitnessGoalService;

    public StepLogDto getStepsForDate(Long userId, LocalDate date) {
        return stepLogRepository.findByUserIdAndLogDate(userId, date)
                .map(this::toDto)
                .orElse(StepLogDto.builder().userId(userId).logDate(date).stepCount(0).build());
    }

    public List<StepLogDto> getStepsForRange(Long userId, LocalDate start, LocalDate end) {
        return stepLogRepository.findByUserIdAndLogDateBetweenOrderByLogDateAsc(userId, start, end)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public StepLogDto logSteps(Long userId, StepLogDto dto) {
        LocalDate date = dto.getLogDate() != null ? dto.getLogDate() : LocalDate.now();

        StepLog log = stepLogRepository.findByUserIdAndLogDate(userId, date)
                .orElse(StepLog.builder().userId(userId).logDate(date).createdAt(LocalDateTime.now()).build());

        if (dto.getStepCount() != null) log.setStepCount(dto.getStepCount());
        if (dto.getDistanceKm() != null) log.setDistanceKm(dto.getDistanceKm());
        if (dto.getActiveMinutes() != null) log.setActiveMinutes(dto.getActiveMinutes());
        log.setSource(dto.getSource() != null ? dto.getSource() : "MANUAL");
        if (dto.getDeviceId() != null) log.setDeviceId(dto.getDeviceId());

        StepLogDto saved = toDto(stepLogRepository.save(log));
        fitnessGoalService.refreshStreak(userId);
        return saved;
    }

    @Transactional
    public List<StepLogDto> syncFromHealthConnect(Long userId, List<StepLogDto> batch) {
        return batch.stream()
                .map(dto -> {
                    dto.setSource("HEALTH_CONNECT");
                    return logSteps(userId, dto);
                })
                .collect(Collectors.toList());
    }

    private StepLogDto toDto(StepLog s) {
        return StepLogDto.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .logDate(s.getLogDate())
                .stepCount(s.getStepCount())
                .distanceKm(s.getDistanceKm())
                .activeMinutes(s.getActiveMinutes())
                .source(s.getSource())
                .deviceId(s.getDeviceId())
                .build();
    }
}
