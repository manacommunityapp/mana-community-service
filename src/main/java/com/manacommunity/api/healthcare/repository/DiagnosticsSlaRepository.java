package com.manacommunity.api.healthcare.repository;

import com.manacommunity.api.healthcare.entity.DiagnosticsSla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface DiagnosticsSlaRepository extends JpaRepository<DiagnosticsSla, Long> {
    List<DiagnosticsSla> findByCommunityIdOrderByReportDueAtAsc(Long communityId);
    List<DiagnosticsSla> findByCommunityIdAndBreach(Long communityId, Boolean breach);

    @Query("SELECT s FROM DiagnosticsSla s WHERE s.reportDeliveredAt IS NULL AND s.reportDueAt < :now AND s.breach = false")
    List<DiagnosticsSla> findUnmarkedBreaches(Instant now);

    @Query("SELECT s FROM DiagnosticsSla s WHERE s.breach = true AND s.breachNotified = false")
    List<DiagnosticsSla> findUnnotifiedBreaches();
}
