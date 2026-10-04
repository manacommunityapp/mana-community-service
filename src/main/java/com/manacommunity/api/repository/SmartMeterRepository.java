package com.manacommunity.api.repository;

import com.manacommunity.api.model.SmartMeter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SmartMeterRepository extends JpaRepository<SmartMeter, Long> {

    List<SmartMeter> findByUnitId(Long unitId);

    List<SmartMeter> findBySocietyId(Long societyId);

    List<SmartMeter> findBySocietyIdAndMeterType(Long societyId, String meterType);

    Optional<SmartMeter> findByMeterNumber(String meterNumber);
}
