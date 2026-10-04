package com.manacommunity.api.repository;

import com.manacommunity.api.model.DiningEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiningEventRepository extends JpaRepository<DiningEvent, Long> {

    List<DiningEvent> findByCommunityIdOrderByDateDescTimeDesc(Long communityId);

    Optional<DiningEvent> findByIdAndCommunityId(Long id, Long communityId);
}
