package com.manacommunity.api.gate.repository;

import com.manacommunity.api.gate.model.RfidTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfidTagMappingRepository extends JpaRepository<RfidTagMapping, Long> {

    Optional<RfidTagMapping> findByCommunityIdAndRfidTagIdAndActiveTrue(
            Long communityId, String rfidTagId);

    List<RfidTagMapping> findByCommunityIdAndActiveTrueOrderByCreatedAtDesc(Long communityId);
}
