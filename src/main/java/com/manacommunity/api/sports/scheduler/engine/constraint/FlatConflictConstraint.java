package com.manacommunity.api.sports.scheduler.engine.constraint;

import com.manacommunity.api.sports.scheduler.engine.dto.ParticipantNode;
import org.springframework.stereotype.Component;

@Component
public class FlatConflictConstraint {

    public boolean hasConflict(ParticipantNode a, ParticipantNode b) {
        if (a == null || b == null || a.isBye() || b.isBye()) return false;
        String flatA = normalize(a.getFlatNumber());
        String flatB = normalize(b.getFlatNumber());
        return !flatA.isEmpty() && flatA.equalsIgnoreCase(flatB);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", "");
    }
}
