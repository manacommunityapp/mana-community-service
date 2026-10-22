package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposCrmTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CposCrmTagRepository extends JpaRepository<CposCrmTag, Long> {
    List<CposCrmTag> findByCommunityId(Long communityId);
    Optional<CposCrmTag> findByCommunityIdAndId(Long communityId, Long id);
    boolean existsByCommunityIdAndNameIgnoreCase(Long communityId, String name);
}
