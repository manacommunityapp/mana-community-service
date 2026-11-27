package com.manacommunity.api.repository;

import com.manacommunity.api.model.WaterPumpRunLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaterPumpRunLogRepository extends JpaRepository<WaterPumpRunLog, Long> {

    List<WaterPumpRunLog> findByPumpIdOrderByStartTimeDesc(Long pumpId);
}
