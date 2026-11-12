package com.manacommunity.api.wellnessbasket.repository;

import com.manacommunity.api.wellnessbasket.entity.WellnessCommunityCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WellnessCommunityClusterRepository extends JpaRepository<WellnessCommunityCluster, Long> {

    Optional<WellnessCommunityCluster> findBySocietyNameIgnoreCase(String societyName);

    List<WellnessCommunityCluster> findByStatus(String status);

    List<WellnessCommunityCluster> findByCityIgnoreCase(String city);
}
