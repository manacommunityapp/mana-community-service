package com.manacommunity.api.release.repository;

import com.manacommunity.api.release.entity.ChaosGamedayExperiment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChaosGamedayExperimentRepository extends JpaRepository<ChaosGamedayExperiment, Long> {
    List<ChaosGamedayExperiment> findByCommunityIdAndStatus(Long communityId, String status);
}
