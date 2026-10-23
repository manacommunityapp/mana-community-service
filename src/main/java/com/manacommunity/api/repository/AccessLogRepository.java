package com.manacommunity.api.repository;

import com.manacommunity.api.model.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    List<AccessLog> findByTurnstileCommunityIdOrderByTimestampDesc(Long communityId);

    List<AccessLog> findByTurnstileCommunityIdAndUserIdOrderByTimestampDesc(Long communityId, Long userId);

    List<AccessLog> findByTurnstileIdOrderByTimestampDesc(Long turnstileId);

    List<AccessLog> findByTurnstileCommunityIdAndTimestampBetweenOrderByTimestampDesc(
            Long communityId, LocalDateTime from, LocalDateTime to);

    long countByTurnstileCommunityIdAndGrantedTrue(Long communityId);

    long countByTurnstileCommunityIdAndGrantedFalse(Long communityId);

    long countByTurnstileCommunityIdAndDirectionAndTimestampAfter(
            Long communityId, AccessLog.AccessDirection direction, LocalDateTime since);
}
