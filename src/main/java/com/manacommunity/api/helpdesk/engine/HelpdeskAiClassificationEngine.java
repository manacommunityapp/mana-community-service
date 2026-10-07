package com.manacommunity.api.helpdesk.engine;

import com.manacommunity.api.helpdesk.dto.HelpdeskAiDtos.AiClassificationResult;
import com.manacommunity.api.helpdesk.entity.Ticket.TicketCategory;
import com.manacommunity.api.helpdesk.entity.Ticket.TicketPriority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Intelligent incident classification, safety hazard detector, and urgency scoring engine.
 * Evaluates semantic keywords and sentiment to recommend categories, priority overrides, and technician skill tags.
 */
@Component
@Slf4j
public class HelpdeskAiClassificationEngine {

    public AiClassificationResult classifyTicket(String subject, String description) {
        return classify(subject, description);
    }

    public AiClassificationResult classify(String subject, String description) {
        String text = ((subject != null ? subject : "") + " " + (description != null ? description : "")).toLowerCase(Locale.ROOT);

        // 1. Critical Hazard & Emergency Detection (Immediate Life / Property Risk)
        if (containsAny(text, "gas leak", "lpg leak", "gas smell", "cylinder leak")) {
            return AiClassificationResult.builder()
                    .category(TicketCategory.GENERAL)
                    .priority(TicketPriority.CRITICAL)
                    .urgencyScore(100)
                    .emergencyHazard(true)
                    .confidence(0.98)
                    .requiredSkills(List.of("SAFETY", "GAS_PIPELINE"))
                    .rootCauseHypothesis("Suspected LPG/piped gas leakage. Evacuate area and turn off main valve immediately.")
                    .build();
        }

        if (containsAny(text, "stuck in lift", "trapped in elevator", "trapped in lift", "lift stuck", "lift failure")) {
            return AiClassificationResult.builder()
                    .category(TicketCategory.ELEVATOR)
                    .priority(TicketPriority.CRITICAL)
                    .urgencyScore(98)
                    .emergencyHazard(true)
                    .confidence(0.96)
                    .requiredSkills(List.of("ELEVATOR_MAINTENANCE", "EMERGENCY_RESCUE"))
                    .rootCauseHypothesis("Passenger entrapment or critical elevator interlock safety fault.")
                    .build();
        }

        if (containsAny(text, "sparking", "fire", "smoke", "burning smell", "short circuit", "electric shock", "exposed live wire")) {
            return AiClassificationResult.builder()
                    .category(TicketCategory.ELECTRICAL)
                    .priority(TicketPriority.CRITICAL)
                    .urgencyScore(95)
                    .emergencyHazard(true)
                    .confidence(0.95)
                    .requiredSkills(List.of("ELECTRICIAN", "HIGH_VOLTAGE"))
                    .rootCauseHypothesis("Severe electrical short circuit or active fire hazard.")
                    .build();
        }

        if (containsAny(text, "pipe burst", "burst pipe", "flooding", "water flooding", "tank overflowed", "sewage backup")) {
            return AiClassificationResult.builder()
                    .category(TicketCategory.PLUMBING)
                    .priority(TicketPriority.CRITICAL)
                    .urgencyScore(88)
                    .emergencyHazard(true)
                    .confidence(0.92)
                    .requiredSkills(List.of("PLUMBER", "DRAINAGE"))
                    .rootCauseHypothesis("High-pressure waterline rupture or major drain line blockage causing flooding.")
                    .build();
        }

        // 2. Domain Category Detection
        TicketCategory category;
        List<String> skills = new ArrayList<>();
        String hypothesis;

        if (containsAny(text, "plumb", "leak", "tap", "faucet", "shower", "flush", "drain", "sink", "clog", "toilet", "geyser", "water")) {
            category = TicketCategory.PLUMBING;
            skills.add("PLUMBER");
            if (containsAny(text, "leak", "dripping", "seepage")) skills.add("SEEPAGE_REPAIR");
            hypothesis = "Plumbing fixture failure or drain line impedance.";
        } else if (containsAny(text, "electric", "power", "light", "mcb", "bulb", "socket", "switch", "wiring", "fan", "inverter", "tripping")) {
            category = TicketCategory.ELECTRICAL;
            skills.add("ELECTRICIAN");
            if (containsAny(text, "mcb", "trip", "voltage")) skills.add("CIRCUIT_BREAKER");
            hypothesis = "Circuit breaker trip, faulty wiring, or defective appliance socket.";
        } else if (containsAny(text, "lift", "elevator")) {
            category = TicketCategory.ELEVATOR;
            skills.add("ELEVATOR_MAINTENANCE");
            hypothesis = "Elevator door sensor misalignment or drive controller error.";
        } else if (containsAny(text, "clean", "garbage", "trash", "dustbin", "sweep", "stink", "smell", "pest", "roach", "cockroach", "corridor")) {
            category = TicketCategory.CLEANLINESS;
            skills.add("HOUSEKEEPING");
            hypothesis = "Housekeeping backlog or common area sanitation lapse.";
        } else if (containsAny(text, "park", "car", "bike", "slot", "vehicle", "blocked")) {
            category = TicketCategory.PARKING;
            skills.add("SECURITY_GUARD");
            hypothesis = "Unauthorized vehicle parking or access bay encroachment.";
        } else if (containsAny(text, "noise", "loud", "music", "barking", "drilling", "disturb")) {
            category = TicketCategory.NOISE;
            skills.add("COMMUNITY_MARSHAL");
            hypothesis = "Noise pollution or quiet hours regulation violation.";
        } else if (containsAny(text, "guard", "security", "theft", "gate", "cctv", "stranger", "break-in")) {
            category = TicketCategory.SECURITY;
            skills.add("SECURITY_GUARD");
            hypothesis = "Gate security non-compliance or perimeter surveillance issue.";
        } else {
            category = TicketCategory.GENERAL;
            skills.add("FACILITY_STAFF");
            hypothesis = "General facility maintenance or administrative service request.";
        }

        // 3. Urgency Scoring
        int urgencyScore = 30; // base score
        if (containsAny(text, "urgent", "asap", "immediately", "critical", "danger", "unbearable", "emergency")) {
            urgencyScore += 30;
        }
        if (containsAny(text, "not working", "broken", "since 2 days", "since yesterday", "recurrent", "again")) {
            urgencyScore += 15;
        }
        if (containsAny(text, "senior", "elderly", "baby", "child", "patient")) {
            urgencyScore += 15;
        }
        urgencyScore = Math.min(100, urgencyScore);

        TicketPriority priority;
        if (urgencyScore >= 80) {
            priority = TicketPriority.HIGH;
        } else if (urgencyScore >= 50) {
            priority = TicketPriority.MEDIUM;
        } else {
            priority = TicketPriority.LOW;
        }

        return AiClassificationResult.builder()
                .category(category)
                .priority(priority)
                .urgencyScore(urgencyScore)
                .emergencyHazard(false)
                .confidence(0.85)
                .requiredSkills(skills)
                .rootCauseHypothesis(hypothesis)
                .build();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
