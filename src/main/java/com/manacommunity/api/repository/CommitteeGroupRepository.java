package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommitteeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommitteeGroupRepository extends JpaRepository<CommitteeGroup, Long> {

    List<CommitteeGroup> findByCommunityIdOrderByDisplayOrderAscNameAsc(Long communityId);

    List<CommitteeGroup> findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAscNameAsc(Long communityId);

    boolean existsByCommunityIdAndName(Long communityId, String name);

    Optional<CommitteeGroup> findByCommunityIdAndId(Long communityId, Long id);
}
