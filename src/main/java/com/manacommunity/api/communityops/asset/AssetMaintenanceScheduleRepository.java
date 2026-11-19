package com.manacommunity.api.communityops.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface AssetMaintenanceScheduleRepository extends JpaRepository<AssetMaintenanceSchedule, Long> {
    List<AssetMaintenanceSchedule> findByAssetId(Long assetId);
    List<AssetMaintenanceSchedule> findByAssetIdAndNextDueBefore(Long assetId, LocalDate date);

    @Query("SELECT s FROM AssetMaintenanceSchedule s JOIN CommunityAsset a ON s.assetId = a.id " +
           "WHERE a.communityId = :communityId AND s.nextDue <= :date")
    List<AssetMaintenanceSchedule> findOverdueForCommunity(Long communityId, LocalDate date);

    @Query("SELECT s FROM AssetMaintenanceSchedule s JOIN CommunityAsset a ON s.assetId = a.id " +
           "WHERE a.communityId = :communityId AND s.nextDue BETWEEN :from AND :to")
    List<AssetMaintenanceSchedule> findUpcomingForCommunity(Long communityId, LocalDate from, LocalDate to);

    @Query("SELECT COUNT(s) FROM AssetMaintenanceSchedule s JOIN CommunityAsset a ON s.assetId = a.id " +
           "WHERE a.communityId = :communityId AND s.nextDue BETWEEN :from AND :to")
    long countByAssetCommunityIdAndNextDueBetween(Long communityId, LocalDate from, LocalDate to);
}
