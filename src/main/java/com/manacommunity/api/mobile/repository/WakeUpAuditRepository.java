package com.manacommunity.api.mobile.repository;

import com.manacommunity.api.mobile.entity.WakeUpAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WakeUpAuditRepository extends JpaRepository<WakeUpAudit, Long> {

    List<WakeUpAudit> findByDeviceId(String deviceId);

    List<WakeUpAudit> findByOem(String oem);

    @Query("SELECT a FROM WakeUpAudit a WHERE a.wakeSuccessful = false ORDER BY a.sentAt DESC")
    List<WakeUpAudit> findRecentFailures();

    @Query("SELECT a.oem, a.strategy, COUNT(a), SUM(CASE WHEN a.wakeSuccessful = true THEN 1 ELSE 0 END) FROM WakeUpAudit a GROUP BY a.oem, a.strategy")
    List<Object[]> getOemStrategyStats();
}
