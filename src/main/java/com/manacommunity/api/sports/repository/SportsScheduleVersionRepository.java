package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsScheduleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SportsScheduleVersionRepository extends JpaRepository<SportsScheduleVersion, Long> {

    List<SportsScheduleVersion> findByConfigIdOrderByVersionNumberDesc(Long configId);

    @Query("SELECT v FROM SportsScheduleVersion v WHERE v.config.id = :configId AND v.stage.id = :stageId ORDER BY v.versionNumber DESC")
    List<SportsScheduleVersion> findByConfigIdAndStageId(@Param("configId") Long configId,
                                                         @Param("stageId") Long stageId);

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM SportsScheduleVersion v WHERE v.config.id = :configId")
    int findMaxVersionNumber(@Param("configId") Long configId);
}
