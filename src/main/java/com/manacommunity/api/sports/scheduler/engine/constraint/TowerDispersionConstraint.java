package com.manacommunity.api.sports.scheduler.engine.constraint;

import com.manacommunity.api.sports.scheduler.engine.dto.ParticipantNode;
import org.springframework.stereotype.Component;

@Component
public class TowerDispersionConstraint {

    public boolean isSameTower(ParticipantNode a, ParticipantNode b) {
        if (a == null || b == null || a.isBye() || b.isBye()) return false;
        String towerA = extractTower(a);
        String towerB = extractTower(b);
        return !towerA.isEmpty() && towerA.equalsIgnoreCase(towerB);
    }

    public String extractTower(ParticipantNode p) {
        if (p.getTower() != null && !p.getTower().isBlank()) {
            return p.getTower().trim().toUpperCase();
        }
        if (p.getFlatNumber() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : p.getFlatNumber().trim().toCharArray()) {
            if (Character.isLetter(c)) sb.append(Character.toUpperCase(c));
            else break;
        }
        return sb.toString();
    }
}
