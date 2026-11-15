package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsTournamentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SportsTournamentStageRepository extends JpaRepository<SportsTournamentStage, Long> {

    List<SportsTournamentStage> findByConfigIdOrderByStageOrder(Long configId);

    @Query("SELECT s FROM SportsTournamentStage s WHERE s.config.id = :configId AND s.stageStatus = :status ORDER BY s.stageOrder")
    List<SportsTournamentStage> findByConfigIdAndStatus(@Param("configId") Long configId,
                                                        @Param("status") SportsTournamentStage.StageStatus status);

    boolean existsByConfigIdAndStageOrder(Long configId, Integer stageOrder);
}
