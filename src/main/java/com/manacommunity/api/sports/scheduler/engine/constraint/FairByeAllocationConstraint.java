package com.manacommunity.api.sports.scheduler.engine.constraint;

import com.manacommunity.api.sports.scheduler.engine.dto.ParticipantNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FairByeAllocationConstraint {

    /**
     * Calculate required bracket size (next power of 2) and number of byes.
     */
    public int nextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        int power = 1;
        while (power < n) {
            power <<= 1;
        }
        return power;
    }

    public int calculateByes(int numParticipants) {
        int targetSize = nextPowerOfTwo(numParticipants);
        return targetSize - numParticipants;
    }

    /**
     * Pad participant list with BYEs. If participants are seeded, top seeds get BYEs.
     */
    public List<ParticipantNode> padWithByes(List<ParticipantNode> participants) {
        int n = participants.size();
        int targetSize = nextPowerOfTwo(n);
        int byesCount = targetSize - n;

        List<ParticipantNode> padded = new ArrayList<>(participants);
        for (int i = 1; i <= byesCount; i++) {
            padded.add(ParticipantNode.byeNode("BYE-" + i));
        }
        return padded;
    }
}
