package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.WorkerGatePassRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository("homeServiceWorkerGatePassRecordRepository")
public interface WorkerGatePassRecordRepository extends JpaRepository<WorkerGatePassRecordEntity, String> {
    Optional<WorkerGatePassRecordEntity> findTopByWorkerIdOrderByCreatedAtDesc(String workerId);
}
