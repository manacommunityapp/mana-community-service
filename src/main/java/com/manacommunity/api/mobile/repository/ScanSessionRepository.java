package com.manacommunity.api.mobile.repository;

import com.manacommunity.api.mobile.entity.ScanSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanSessionRepository extends JpaRepository<ScanSession, Long> {

    List<ScanSession> findByGuardId(String guardId);

    List<ScanSession> findByGateId(String gateId);

    @Query("SELECT s FROM ScanSession s WHERE s.avgDecodeMs > :thresholdMs")
    List<ScanSession> findSlowDecodeSessions(double thresholdMs);

    @Query("SELECT s FROM ScanSession s WHERE s.thermalThrottleEvents > 0 ORDER BY s.thermalThrottleEvents DESC")
    List<ScanSession> findThermalThrottledSessions();
}
