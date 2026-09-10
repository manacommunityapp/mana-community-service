package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsMatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SportsMatchResultRepository extends JpaRepository<SportsMatchResult, Long> {
    Optional<SportsMatchResult> findByMatchId(Long matchId);
    List<SportsMatchResult> findByMatch_Config_Id(Long configId);
}
