package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.KarateProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KarateProgramRepository extends JpaRepository<KarateProgram, Long> {
    List<KarateProgram> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
}
