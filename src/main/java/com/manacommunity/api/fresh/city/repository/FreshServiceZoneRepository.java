package com.manacommunity.api.fresh.city.repository;

import com.manacommunity.api.fresh.city.model.FreshServiceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshServiceZoneRepository extends JpaRepository<FreshServiceZone, Long> {
    List<FreshServiceZone> findByCityIdAndIsActiveTrue(Long cityId);
    Optional<FreshServiceZone> findByCityIdAndCode(Long cityId, String code);
    List<FreshServiceZone> findByIsActiveTrue();
}
