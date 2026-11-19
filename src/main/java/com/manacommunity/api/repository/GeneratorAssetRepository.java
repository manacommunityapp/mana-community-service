package com.manacommunity.api.repository;

import com.manacommunity.api.model.GeneratorAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeneratorAssetRepository extends JpaRepository<GeneratorAsset, Long> {

    List<GeneratorAsset> findByCommunityIdOrderByNameAsc(Long communityId);

    Optional<GeneratorAsset> findByCommunityIdAndId(Long communityId, Long id);

    Optional<GeneratorAsset> findByCommunityIdAndSerialNumber(Long communityId, String serialNumber);
}
