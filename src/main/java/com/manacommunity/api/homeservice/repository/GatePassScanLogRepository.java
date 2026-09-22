package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.GatePassScanLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository("homeServiceGatePassScanLogRepository")
public interface GatePassScanLogRepository extends JpaRepository<GatePassScanLogEntity, String> {
    List<GatePassScanLogEntity> findByWorkerIdOrderByScanTimeDesc(String workerId);
    List<GatePassScanLogEntity> findByGatePassIdOrderByScanTimeDesc(String gatePassId);
}
