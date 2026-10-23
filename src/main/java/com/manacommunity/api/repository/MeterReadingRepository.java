package com.manacommunity.api.repository;

import com.manacommunity.api.model.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    List<MeterReading> findByMeterIdOrderByReadingDateDesc(Long meterId);

    List<MeterReading> findByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(
            Long meterId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(r.consumption), 0) FROM MeterReading r " +
           "WHERE r.meter.communityId = :cid AND r.meter.meterType = :type " +
           "AND MONTH(r.readingDate) = :month AND YEAR(r.readingDate) = :year")
    BigDecimal sumConsumptionByCommunityAndTypeAndMonth(@Param("cid") Long communityId,
                                                        @Param("type") com.manacommunity.api.model.SmartMeter.MeterType type,
                                                        @Param("month") int month,
                                                        @Param("year") int year);

    @Query("SELECT r FROM MeterReading r WHERE r.meter.communityId = :cid " +
           "AND r.consumption > :threshold ORDER BY r.consumption DESC")
    List<MeterReading> findHighConsumptionAlerts(@Param("cid") Long communityId,
                                                 @Param("threshold") BigDecimal threshold);
}
