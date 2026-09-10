package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsGroupTeamStanding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SportsGroupTeamStandingRepository extends JpaRepository<SportsGroupTeamStanding, Long> {

    @Query("SELECT s FROM SportsGroupTeamStanding s WHERE s.group.id=:gid ORDER BY s.points DESC, s.netRunRate DESC")
    List<SportsGroupTeamStanding> findByGroupIdOrderByPointsDescNetRunRateDesc(@Param("gid") Long groupId);

    @Query("SELECT s FROM SportsGroupTeamStanding s WHERE s.group.id=:gid AND s.team.id=:tid")
    SportsGroupTeamStanding findByGroupIdAndTeamId(@Param("gid") Long groupId, @Param("tid") Long teamId);
}
