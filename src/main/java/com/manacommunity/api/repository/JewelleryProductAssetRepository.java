package com.manacommunity.api.repository;

import com.manacommunity.api.model.JewelleryProduct;
import com.manacommunity.api.model.JewelleryProductAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JewelleryProductAssetRepository extends JpaRepository<JewelleryProductAsset, Long> {

    List<JewelleryProductAsset> findByProductOrderByAssetVersionDesc(JewelleryProduct product);

    List<JewelleryProductAsset> findByProductAndApprovalStatus(JewelleryProduct product, String approvalStatus);

    List<JewelleryProductAsset> findByProductAndAssetTypeAndApprovalStatus(
            JewelleryProduct product, String assetType, String approvalStatus);
}
