package com.manacommunity.api.sportsops.service;

import com.manacommunity.api.sportsops.entity.OpenPlayParticipant;
import com.manacommunity.api.sportsops.entity.OpenPlaySession;
import com.manacommunity.api.sportsops.repository.OpenPlayParticipantRepository;
import com.manacommunity.api.sportsops.repository.OpenPlaySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenPlayService {
    private final OpenPlaySessionRepository sessionRepo;
    private final OpenPlayParticipantRepository participantRepo;

    public List<OpenPlaySession> listOpen(Long communityId) {
        return sessionRepo.findByCommunityIdAndStatus(communityId, "OPEN");
    }

    public List<OpenPlaySession> listAll(Long communityId) {
        return sessionRepo.findByCommunityId(communityId);
    }

    @Transactional
    public OpenPlaySession create(OpenPlaySession session) {
        return sessionRepo.save(session);
    }

    @Transactional
    public OpenPlayParticipant join(Long communityId, Long sessionId, Long userId) {
        OpenPlaySession session = sessionRepo.findById(sessionId)
            .filter(s -> s.getCommunityId().equals(communityId) && "OPEN".equals(s.getStatus()))
            .orElseThrow(() -> new IllegalArgumentException("Session not available"));
        int count = participantRepo.countBySessionId(sessionId);
        if (count >= session.getMaxPlayers()) {
            throw new IllegalStateException("Session is full");
        }
        if (participantRepo.findBySessionIdAndUserId(sessionId, userId).isPresent()) {
            throw new IllegalStateException("Already joined");
        }
        OpenPlayParticipant p = new OpenPlayParticipant();
        p.setSessionId(sessionId);
        p.setUserId(userId);
        return participantRepo.save(p);
    }

    @Transactional
    public OpenPlaySession close(Long communityId, Long sessionId) {
        OpenPlaySession session = sessionRepo.findById(sessionId)
            .filter(s -> s.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.setStatus("CLOSED");
        return sessionRepo.save(session);
    }

    public List<OpenPlayParticipant> participants(Long sessionId) {
        return participantRepo.findBySessionId(sessionId);
    }
}
