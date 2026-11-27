package com.manacommunity.api.repository;

import com.manacommunity.api.model.WaterPump;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaterPumpRepository extends JpaRepository<WaterPump, Long> {

    List<WaterPump> findByCommunityIdOrderByNameAsc(Long communityId);

    Optional<WaterPump> findByCommunityIdAndId(Long communityId, Long id);
}
