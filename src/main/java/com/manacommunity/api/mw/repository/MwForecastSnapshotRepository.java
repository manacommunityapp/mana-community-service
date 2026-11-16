package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwForecastSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MwForecastSnapshotRepository extends JpaRepository<MwForecastSnapshot, Long> {

    List<MwForecastSnapshot> findByOrganizationIdOrderByWeekStartDateDesc(Long orgId);

    List<MwForecastSnapshot> findByOrganizationIdAndSourceModuleOrderByWeekStartDateAsc(Long orgId, String sourceModule);

    @Query("SELECT s FROM MwForecastSnapshot s WHERE s.organizationId = :orgId AND s.weekStartDate >= :since ORDER BY s.weekStartDate ASC")
    List<MwForecastSnapshot> findRecent(@Param("orgId") Long orgId, @Param("since") LocalDate since);

    Optional<MwForecastSnapshot> findByOrganizationIdAndWeekStartDateAndSourceModule(
            Long orgId, LocalDate weekStartDate, String sourceModule);
}
