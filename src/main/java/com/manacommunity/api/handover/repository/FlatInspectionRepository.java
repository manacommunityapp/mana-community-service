package com.manacommunity.api.handover.repository;

import com.manacommunity.api.handover.entity.FlatInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlatInspectionRepository extends JpaRepository<FlatInspection, Long> {

    List<FlatInspection> findByFlatNumberAndTowerName(String flatNumber, String towerName);

    @Query("SELECT i FROM FlatInspection i WHERE i.possessionSignOffBlocked = true ORDER BY i.scheduledAt DESC")
    List<FlatInspection> findBlockedInspections();

    @Query("SELECT i FROM FlatInspection i WHERE i.outcome = 'FAIL' ORDER BY i.completedAt DESC")
    List<FlatInspection> findFailedInspections();

    @Query("SELECT i FROM FlatInspection i WHERE i.buyerSignedOff = true ORDER BY i.buyerSignOffAt DESC")
    List<FlatInspection> findCompletedHandovers();
}
