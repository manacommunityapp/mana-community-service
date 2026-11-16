package com.manacommunity.api.fresh.processing.repository;

import com.manacommunity.api.fresh.processing.model.FreshBatchTemperatureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FreshBatchTemperatureLogRepository extends JpaRepository<FreshBatchTemperatureLog, Long> {
    List<FreshBatchTemperatureLog> findByBatchIdOrderByLoggedAtAsc(Long batchId);
}