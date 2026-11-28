package com.manacommunity.api.assetaudit.repository;

import com.manacommunity.api.assetaudit.entity.AssetInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetInspectionRepository extends JpaRepository<AssetInspection, Long> {

    List<AssetInspection> findByAssetIdOrderByInspectedAtDesc(Long assetId);

    @Query("SELECT i FROM AssetInspection i WHERE i.result IN ('FAIL', 'CRITICAL', 'NEEDS_REPLACEMENT') ORDER BY i.inspectedAt DESC")
    List<AssetInspection> findCriticalFindings();

    List<AssetInspection> findByTechnicianId(String technicianId);

    @Query("SELECT i FROM AssetInspection i WHERE i.tamperDetected = true ORDER BY i.inspectedAt DESC")
    List<AssetInspection> findTamperEvents();
}
