package com.manacommunity.api.repository;

import com.manacommunity.api.model.AssetAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetAuditLogRepository extends JpaRepository<AssetAuditLog, Long> {
    List<AssetAuditLog> findByAssetIdOrderByAuditedAtDesc(Long assetId);
}
