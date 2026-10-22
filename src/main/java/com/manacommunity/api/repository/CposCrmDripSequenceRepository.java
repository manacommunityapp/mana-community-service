package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposCrmDripSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CposCrmDripSequenceRepository extends JpaRepository<CposCrmDripSequence, Long> {
    List<CposCrmDripSequence> findByCommunityIdOrderByNameAsc(Long communityId);
    Optional<CposCrmDripSequence> findByCommunityIdAndId(Long communityId, Long id);
    List<CposCrmDripSequence> findByCommunityIdAndTriggerStageAndActiveTrue(Long communityId, String triggerStage);
}
