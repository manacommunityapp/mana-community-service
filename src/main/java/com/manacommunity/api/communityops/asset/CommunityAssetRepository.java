package com.manacommunity.api.communityops.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommunityAssetRepository extends JpaRepository<CommunityAsset, Long> {
    List<CommunityAsset> findByCommunityId(Long communityId);
    List<CommunityAsset> findByCommunityIdAndStatus(Long communityId, AssetStatus status);
    List<CommunityAsset> findByCommunityIdAndCategory(Long communityId, AssetCategory category);
}
