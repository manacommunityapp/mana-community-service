package com.manacommunity.api.sports.scheduler.engine.constraint;

import com.manacommunity.api.sports.scheduler.engine.dto.ScheduledMatchNode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class RestPeriodConstraint {

    public boolean validateRestPeriods(List<ScheduledMatchNode> matches, int minRestMinutes, List<String> violations) {
        Map<String, List<ScheduledMatchNode>> participantSchedule = new HashMap<>();

        for (ScheduledMatchNode m : matches) {
            if (m.isByeMatch() || m.getStartTime() == null) continue;
            if (m.getParticipantA() != null && !m.getParticipantA().isBye() && m.getParticipantA().getId() != null) {
                participantSchedule.computeIfAbsent(m.getParticipantA().getId(), k -> new ArrayList<>()).add(m);
            }
            if (m.getParticipantB() != null && !m.getParticipantB().isBye() && m.getParticipantB().getId() != null) {
                participantSchedule.computeIfAbsent(m.getParticipantB().getId(), k -> new ArrayList<>()).add(m);
            }
        }

        boolean allValid = true;
        for (Map.Entry<String, List<ScheduledMatchNode>> entry : participantSchedule.entrySet()) {
            List<ScheduledMatchNode> pMatches = entry.getValue();
            pMatches.sort(Comparator.comparing(ScheduledMatchNode::getStartTime));

            for (int i = 0; i < pMatches.size() - 1; i++) {
                ScheduledMatchNode curr = pMatches.get(i);
                ScheduledMatchNode next = pMatches.get(i + 1);

                long restMins = Duration.between(curr.getEndTime(), next.getStartTime()).toMinutes();
                if (restMins < minRestMinutes) {
                    allValid = false;
                    violations.add(String.format("Participant %s has only %d mins rest between Match %s and %s (min required: %d mins)",
                            entry.getKey(), restMins, curr.getMatchName(), next.getMatchName(), minRestMinutes));
                }
            }
        }
        return allValid;
    }
}
