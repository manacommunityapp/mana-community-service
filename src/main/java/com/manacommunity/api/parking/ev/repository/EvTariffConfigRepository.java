package com.manacommunity.api.parking.ev.repository;

import com.manacommunity.api.parking.ev.entity.EvTariffConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvTariffConfigRepository extends JpaRepository<EvTariffConfig, Long> {
    Optional<EvTariffConfig> findByCommunityIdAndActiveTrue(Long communityId);
}
