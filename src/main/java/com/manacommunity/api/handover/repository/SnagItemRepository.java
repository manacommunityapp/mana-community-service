package com.manacommunity.api.handover.repository;

import com.manacommunity.api.handover.entity.SnagItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SnagItemRepository extends JpaRepository<SnagItem, Long> {

    Optional<SnagItem> findBySnagCode(String snagCode);

    List<SnagItem> findByFlatNumberAndTowerName(String flatNumber, String towerName);

    List<SnagItem> findByAssignedSubcontractorId(String subcontractorId);

    @Query("SELECT s FROM SnagItem s WHERE s.severity = 'CRITICAL' AND s.status NOT IN ('CLOSED', 'RE_INSPECTION_PASSED') ORDER BY s.reportedAt ASC")
    List<SnagItem> findOpenCriticalSnags();

    @Query("SELECT s FROM SnagItem s WHERE s.possessionBlocker = true AND s.status NOT IN ('CLOSED', 'RE_INSPECTION_PASSED') ORDER BY s.severity DESC")
    List<SnagItem> findPossessionBlockers();

    @Query("SELECT s FROM SnagItem s WHERE s.status = 'DISPUTED' ORDER BY s.reportedAt DESC")
    List<SnagItem> findDisputedSnags();

    @Query("SELECT s FROM SnagItem s WHERE s.status IN ('LOGGED', 'ACKNOWLEDGED') AND s.assignedSubcontractorId IS NULL")
    List<SnagItem> findUnassignedSnags();

    @Query("SELECT s.category, COUNT(s) FROM SnagItem s GROUP BY s.category")
    List<Object[]> countByCategory();

    @Query("SELECT s.status, COUNT(s) FROM SnagItem s GROUP BY s.status")
    List<Object[]> countByStatus();

    @Query("SELECT s.towerName, s.flatNumber, COUNT(s) FROM SnagItem s GROUP BY s.towerName, s.flatNumber ORDER BY COUNT(s) DESC")
    List<Object[]> countByFlat();
}
