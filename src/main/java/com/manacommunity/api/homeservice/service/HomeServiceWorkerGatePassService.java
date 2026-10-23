package com.manacommunity.api.homeservice.service;

import com.manacommunity.api.homeservice.dto.HomeServiceGatePassScanRequest;
import com.manacommunity.api.homeservice.model.entity.GatePassScanLogEntity;
import com.manacommunity.api.homeservice.model.entity.WorkerGatePassRecordEntity;
import com.manacommunity.api.homeservice.model.enums.HomeServiceGatePassStatus;
import com.manacommunity.api.homeservice.repository.GatePassScanLogRepository;
import com.manacommunity.api.homeservice.repository.WorkerGatePassRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service("homeServiceWorkerGatePassService")
@RequiredArgsConstructor
public class HomeServiceWorkerGatePassService {
    private final WorkerGatePassRecordRepository gatePassRepository;
    private final GatePassScanLogRepository scanLogRepository;

    public WorkerGatePassRecordEntity getGatePass(String workerId, String communityId) {
        return gatePassRepository.findTopByWorkerIdOrderByCreatedAtDesc(workerId)
                .orElseGet(() -> {
                    WorkerGatePassRecordEntity pass = WorkerGatePassRecordEntity.builder()
                            .id(UUID.randomUUID().toString())
                            .workerId(workerId)
                            .communityId(communityId)
                            .validFrom(LocalDate.now())
                            .validTo(LocalDate.now().plusMonths(6))
                            .qrTokenHash("MANA-SEC-QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .status(HomeServiceGatePassStatus.ACTIVE)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return gatePassRepository.save(pass);
                });
    }

    @Transactional
    public GatePassScanLogEntity recordScan(HomeServiceGatePassScanRequest req) {
        GatePassScanLogEntity log = GatePassScanLogEntity.builder()
                .id(UUID.randomUUID().toString())
                .gatePassId(req.getGatePassId())
                .workerId(req.getWorkerId())
                .scanType(req.getScanType())
                .gateName(req.getGateName())
                .guardUserId(req.getGuardUserId())
                .guardName(req.getGuardName())
                .scanTime(LocalDateTime.now())
                .build();
        return scanLogRepository.save(log);
    }

    public List<GatePassScanLogEntity> getScanHistory(String workerId) {
        return scanLogRepository.findByWorkerIdOrderByScanTimeDesc(workerId);
    }
}
