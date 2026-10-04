package com.manacommunity.api.repository;

import com.manacommunity.api.model.UtilityConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilityConsumptionRepository extends JpaRepository<UtilityConsumption, Long> {

    Optional<UtilityConsumption> findByUnitIdAndCycleMonth(Long unitId, String cycleMonth);
}
