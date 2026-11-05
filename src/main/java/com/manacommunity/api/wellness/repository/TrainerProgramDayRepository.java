package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.TrainerProgramDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainerProgramDayRepository extends JpaRepository<TrainerProgramDay, Long> {
    List<TrainerProgramDay> findByProgramIdOrderByWeekNumberAscDayNumberAsc(Long programId);
    Optional<TrainerProgramDay> findByProgramIdAndWeekNumberAndDayNumber(Long programId, Integer week, Integer day);
}
