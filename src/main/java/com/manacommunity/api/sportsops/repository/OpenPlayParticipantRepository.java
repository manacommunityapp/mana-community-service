package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.OpenPlayParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OpenPlayParticipantRepository extends JpaRepository<OpenPlayParticipant, Long> {
    List<OpenPlayParticipant> findBySessionId(Long sessionId);
    Optional<OpenPlayParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);
    int countBySessionId(Long sessionId);
}
