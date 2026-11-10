package com.manacommunity.api.broadcast.repository;

import com.manacommunity.api.broadcast.entity.Broadcast;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {
    List<Broadcast> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
}
