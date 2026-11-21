package com.manacommunity.api.communityops.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AssetMaintenanceLogRepository extends JpaRepository<AssetMaintenanceLog, Long> {
    List<AssetMaintenanceLog> findByAssetIdOrderByScheduledDateDesc(Long assetId);
    List<AssetMaintenanceLog> findByStatus(MaintenanceLogStatus status);

    @Query("SELECT COUNT(l) FROM AssetMaintenanceLog l JOIN CommunityAsset a ON l.assetId = a.id " +
           "WHERE a.communityId = :communityId AND l.status = :status")
    long countByCommunityIdAndStatus(Long communityId, MaintenanceLogStatus status);
}
