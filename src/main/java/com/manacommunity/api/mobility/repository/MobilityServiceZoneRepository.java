package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityServiceZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MobilityServiceZoneRepository extends JpaRepository<MobilityServiceZone, Long> {
    List<MobilityServiceZone> findByCommunityIdAndActiveTrueOrderByNameAsc(Long communityId);
}
