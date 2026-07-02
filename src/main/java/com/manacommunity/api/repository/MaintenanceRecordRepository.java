package com.manacommunity.api.repository;

import com.manacommunity.api.model.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByAssetIdOrderByMaintenanceDateDesc(Long assetId);
}
