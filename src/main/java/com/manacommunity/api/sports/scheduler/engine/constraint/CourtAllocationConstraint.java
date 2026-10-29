package com.manacommunity.api.sports.scheduler.engine.constraint;

import com.manacommunity.api.sports.model.SportsCourt;
import com.manacommunity.api.sports.scheduler.engine.dto.ScheduledMatchNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CourtAllocationConstraint {

    public boolean validateCourtOverlaps(List<ScheduledMatchNode> matches, List<String> violations) {
        Map<Long, List<ScheduledMatchNode>> courtMatches = new HashMap<>();

        for (ScheduledMatchNode m : matches) {
            if (m.isByeMatch() || m.getCourtId() == null || m.getStartTime() == null) continue;
            courtMatches.computeIfAbsent(m.getCourtId(), k -> new ArrayList<>()).add(m);
        }

        boolean valid = true;
        for (Map.Entry<Long, List<ScheduledMatchNode>> entry : courtMatches.entrySet()) {
            List<ScheduledMatchNode> cList = entry.getValue();
            cList.sort(Comparator.comparing(ScheduledMatchNode::getStartTime));

            for (int i = 0; i < cList.size() - 1; i++) {
                ScheduledMatchNode m1 = cList.get(i);
                ScheduledMatchNode m2 = cList.get(i + 1);

                if (m1.getEndTime().isAfter(m2.getStartTime())) {
                    valid = false;
                    violations.add(String.format("Court %s double-booked: Match %s (%s to %s) overlaps with Match %s (%s to %s)",
                            m1.getCourtName(), m1.getMatchName(), m1.getStartTime(), m1.getEndTime(),
                            m2.getMatchName(), m2.getStartTime(), m2.getEndTime()));
                }
            }
        }
        return valid;
    }
}
