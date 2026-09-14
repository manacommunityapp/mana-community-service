package com.manacommunity.api.repository.karate;

import com.manacommunity.api.model.karate.SportsKarateProgram;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsKarateProgramRepository extends JpaRepository<SportsKarateProgram, Long> {

    Page<SportsKarateProgram> findByCommunityId(Long communityId, Pageable pageable);

    List<SportsKarateProgram> findByCommunityIdAndActiveTrue(Long communityId);

    Page<SportsKarateProgram> findByCommunityIdAndActive(Long communityId, Boolean active, Pageable pageable);

    boolean existsByIdAndCommunityId(Long id, Long communityId);
}
