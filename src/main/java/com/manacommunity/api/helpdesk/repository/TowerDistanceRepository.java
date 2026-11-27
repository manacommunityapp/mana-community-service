package com.manacommunity.api.helpdesk.repository;

import com.manacommunity.api.helpdesk.entity.TowerDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TowerDistanceRepository extends JpaRepository<TowerDistance, Long> {

    @Query("SELECT d FROM TowerDistance d WHERE d.communityId = :communityId AND ((d.towerA = :a AND d.towerB = :b) OR (d.towerA = :b AND d.towerB = :a))")
    Optional<TowerDistance> findDistance(@Param("communityId") Long communityId, @Param("a") String a, @Param("b") String b);
}
