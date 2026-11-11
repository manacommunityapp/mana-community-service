package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwForecastProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MwForecastProjectionRepository extends JpaRepository<MwForecastProjection, Long> {

    List<MwForecastProjection> findByOrganizationIdOrderByProjectedWeekAsc(Long orgId);

    List<MwForecastProjection> findByOrganizationIdAndSourceModuleOrderByProjectedWeekAsc(Long orgId, String sourceModule);

    void deleteByOrganizationIdAndProjectedWeekAfter(Long orgId, LocalDate after);
}
