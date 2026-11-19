package com.manacommunity.api.repository;

import com.manacommunity.api.model.TankCleaningSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TankCleaningScheduleRepository extends JpaRepository<TankCleaningSchedule, Long> {

    List<TankCleaningSchedule> findByTankIdOrderByNextDueDateAsc(Long tankId);
}
