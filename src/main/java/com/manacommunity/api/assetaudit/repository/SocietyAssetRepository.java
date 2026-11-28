package com.manacommunity.api.assetaudit.repository;

import com.manacommunity.api.assetaudit.entity.SocietyAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SocietyAssetRepository extends JpaRepository<SocietyAsset, Long> {

    Optional<SocietyAsset> findByQrCode(String qrCode);

    List<SocietyAsset> findByCommunityIdAndCategory(Long communityId, SocietyAsset.AssetCategory category);

    @Query("SELECT a FROM SocietyAsset a WHERE a.nextInspectionDue <= :date AND a.status = 'ACTIVE'")
    List<SocietyAsset> findOverdueInspections(LocalDate date);

    @Query("SELECT a FROM SocietyAsset a WHERE a.warrantyEndDate <= :date AND a.warrantyEndDate >= :startDate AND a.status = 'ACTIVE'")
    List<SocietyAsset> findExpiringWarranties(LocalDate startDate, LocalDate date);

    @Query("SELECT a FROM SocietyAsset a WHERE a.complianceStatus IN ('OVERDUE', 'NON_COMPLIANT')")
    List<SocietyAsset> findNonCompliantAssets();

    List<SocietyAsset> findByTowerName(String towerName);
}
