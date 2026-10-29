package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsTrainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsTrainerRepository extends JpaRepository<SportsTrainer, Long> {

    List<SportsTrainer> findByCommunity_IdAndStatusOrderByCreatedAtDesc(Long communityId, String status);

    List<SportsTrainer> findByCommunity_IdOrderByCreatedAtDesc(Long communityId);

    boolean existsByCommunity_IdAndUserIdAndStatusNot(Long communityId, Long userId, String status);
}
