package com.manacommunity.api.iot.metering;

import com.manacommunity.api.iot.metering.MeteringEnums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmartMeterRepository extends JpaRepository<SmartMeter, Long> {
    Optional<SmartMeter> findByMeterSerialNumber(String serialNumber);
    List<SmartMeter> findByCommunityId(Long communityId);
    List<SmartMeter> findByCommunityIdAndMeterType(Long communityId, MeterType meterType);
    Optional<SmartMeter> findByCommunityIdAndUnitNumberAndMeterType(Long communityId, String unitNumber, MeterType meterType);
}
