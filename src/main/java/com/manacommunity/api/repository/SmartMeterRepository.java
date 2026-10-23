package com.manacommunity.api.repository;

import com.manacommunity.api.model.SmartMeter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SmartMeterRepository extends JpaRepository<SmartMeter, Long> {

    List<SmartMeter> findByCommunityIdOrderByFlatNumber(Long communityId);

    List<SmartMeter> findByCommunityIdAndFlatNumberOrderByMeterType(Long communityId, String flatNumber);

    List<SmartMeter> findByCommunityIdAndMeterTypeOrderByFlatNumber(Long communityId, SmartMeter.MeterType meterType);

    Optional<SmartMeter> findByIdAndCommunityId(Long id, Long communityId);
}
