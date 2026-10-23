package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsCricHeroesProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsCricHeroesProfileRepository extends JpaRepository<SportsCricHeroesProfile, Long> {

    Optional<SportsCricHeroesProfile> findByPlayerId(Long playerId);

    Optional<SportsCricHeroesProfile> findByCricheroesIdAndCommunityId(String cricheroesId, Long communityId);

    @Query("SELECT p FROM SportsCricHeroesProfile p WHERE p.player.config.id = :configId")
    List<SportsCricHeroesProfile> findByConfigId(@Param("configId") Long configId);

    boolean existsByPlayerId(Long playerId);

    void deleteByPlayerId(Long playerId);

    @Query("SELECT COUNT(p) FROM SportsCricHeroesProfile p WHERE p.player.config.id = :configId")
    long countByConfigId(@Param("configId") Long configId);
}
