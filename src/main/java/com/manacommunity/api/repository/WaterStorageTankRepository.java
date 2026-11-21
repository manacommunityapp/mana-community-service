package com.manacommunity.api.repository;

import com.manacommunity.api.model.WaterStorageTank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaterStorageTankRepository extends JpaRepository<WaterStorageTank, Long> {

    List<WaterStorageTank> findByCommunityIdOrderByNameAsc(Long communityId);

    Optional<WaterStorageTank> findByCommunityIdAndId(Long communityId, Long id);

    Optional<WaterStorageTank> findByCommunityIdAndTankType(Long communityId, String tankType);
}
