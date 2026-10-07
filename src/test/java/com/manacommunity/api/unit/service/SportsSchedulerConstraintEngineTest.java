package com.manacommunity.api.unit.service;

import com.manacommunity.api.sports.model.SportsCourt;
import com.manacommunity.api.sports.scheduler.engine.SportsSchedulerConstraintEngine;
import com.manacommunity.api.sports.scheduler.engine.constraint.*;
import com.manacommunity.api.sports.scheduler.engine.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SportsSchedulerConstraintEngineTest {

    private SportsSchedulerConstraintEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SportsSchedulerConstraintEngine(
                new FlatConflictConstraint(),
                new TowerDispersionConstraint(),
                new FairByeAllocationConstraint(),
                new BalancedBracketConstraint(),
                new RestPeriodConstraint(),
                new CourtAllocationConstraint(),
                new TimeSlotAllocationConstraint(),
                new RatingBalancingConstraint()
        );
    }

    @Test
    void testConstraint1And2_FlatAndTowerConflictAvoidance() {
        // Setup 4 players: 2 in Flat A-302, 2 in Flat B-101
        List<ParticipantNode> players = List.of(
                ParticipantNode.builder().id("P1").name("Amit (A302)").flatNumber("A-302").tower("A").rating(1200.0).build(),
                ParticipantNode.builder().id("P2").name("Sumit (A302)").flatNumber("A-302").tower("A").rating(1100.0).build(),
                ParticipantNode.builder().id("P3").name("Rahul (B101)").flatNumber("B-101").tower("B").rating(1000.0).build(),
                ParticipantNode.builder().id("P4").name("Vikas (B101)").flatNumber("B-101").tower("B").rating(900.0).build()
        );

        List<SportsCourt> courts = List.of(
                SportsCourt.builder().id(1L).name("Court 1").build(),
                SportsCourt.builder().id(2L).name("Court 2").build()
        );

        ScheduleConstraintOptions options = ScheduleConstraintOptions.builder()
                .enforceFlatSeparation(true)
                .enforceTowerDispersion(true)
                .build();

        List<ScheduledMatchNode> matches = engine.generateAuthoritativeSchedule(players, courts, options);

        // Verify Round 1 matches
        ScheduledMatchNode m1 = matches.get(0);
        ScheduledMatchNode m2 = matches.get(1);

        assertNotEquals(m1.getParticipantA().getFlatNumber(), m1.getParticipantB().getFlatNumber(), "R1 Match 1 must not pair same flat");
        assertNotEquals(m2.getParticipantA().getFlatNumber(), m2.getParticipantB().getFlatNumber(), "R1 Match 2 must not pair same flat");
    }

    @Test
    void testConstraint3_FairByeAllocation_NonPowerOfTwo() {
        // 5 participants -> Target bracket 8 -> 3 BYEs
        List<ParticipantNode> players = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            players.add(ParticipantNode.builder().id("P" + i).name("Player " + i).rating((double) (1000 + i * 50)).build());
        }

        List<SportsCourt> courts = List.of(SportsCourt.builder().id(1L).name("Court 1").build());
        ScheduleConstraintOptions options = ScheduleConstraintOptions.builder().build();

        List<ScheduledMatchNode> matches = engine.generateAuthoritativeSchedule(players, courts, options);

        // In an 8-player bracket there are 4 matches in R1
        long r1Matches = matches.stream().filter(m -> m.getRoundIndex() == 0).count();
        assertEquals(4, r1Matches);

        long byeMatches = matches.stream().filter(m -> m.getRoundIndex() == 0 && m.isByeMatch()).count();
        assertEquals(3, byeMatches, "Exact 3 BYE matches must be allocated for 5 players in bracket of 8");
    }

    @Test
    void testConstraint4_BalancedBracketStructure() {
        // 8 participants -> 4 R1 matches, 2 Semis, 1 Final (Total 7 matches)
        List<ParticipantNode> players = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            players.add(ParticipantNode.builder().id("P" + i).name("Player " + i).rating((double) (1000 + i * 10)).build());
        }

        List<SportsCourt> courts = List.of(SportsCourt.builder().id(1L).name("Court 1").build());
        ScheduleConstraintOptions options = ScheduleConstraintOptions.builder().build();

        List<ScheduledMatchNode> matches = engine.generateAuthoritativeSchedule(players, courts, options);

        assertEquals(7, matches.size(), "8 player bracket must produce 7 total match nodes");

        ScheduledMatchNode finalMatch = matches.get(matches.size() - 1);
        assertEquals("Final", finalMatch.getRoundName());
    }

    @Test
    void testConstraint6And7_CourtAndTimeslotAllocation() {
        List<ParticipantNode> players = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            players.add(ParticipantNode.builder().id("P" + i).name("Player " + i).flatNumber("F" + i).build());
        }

        List<SportsCourt> courts = List.of(
                SportsCourt.builder().id(1L).name("Court 1").build(),
                SportsCourt.builder().id(2L).name("Court 2").build()
        );

        ScheduleConstraintOptions options = ScheduleConstraintOptions.builder()
                .matchDurationMinutes(45)
                .changeoverBufferMinutes(15)
                .dayStartTime(LocalTime.of(9, 0))
                .tournamentStartDate(LocalDate.of(2026, 10, 10))
                .build();

        List<ScheduledMatchNode> matches = engine.generateAuthoritativeSchedule(players, courts, options);

        ScheduledMatchNode m1 = matches.get(0);
        ScheduledMatchNode m2 = matches.get(1);

        assertNotNull(m1.getStartTime());
        assertNotNull(m1.getEndTime());
        assertEquals(45, java.time.Duration.between(m1.getStartTime(), m1.getEndTime()).toMinutes());

        // Validate audit report
        ConstraintValidationReport report = engine.validateSchedule(matches, options);
        assertTrue(report.isValid(), "Schedule must pass court overlap & rest validation");
        assertEquals(0, report.getCourtOverlapViolations());
    }

    @Test
    void testConstraint8_RatingBalancingAndSeeding() {
        // Top 2 seeds must be placed in opposite halves of the bracket
        List<ParticipantNode> players = List.of(
                ParticipantNode.builder().id("P1").name("Seed 1").rating(2000.0).build(),
                ParticipantNode.builder().id("P2").name("Seed 2").rating(1900.0).build(),
                ParticipantNode.builder().id("P3").name("Seed 3").rating(1500.0).build(),
                ParticipantNode.builder().id("P4").name("Seed 4").rating(1400.0).build()
        );

        List<SportsCourt> courts = List.of(SportsCourt.builder().id(1L).name("Court 1").build());
        ScheduleConstraintOptions options = ScheduleConstraintOptions.builder()
                .enforceRatingBalancing(true)
                .build();

        List<ScheduledMatchNode> matches = engine.generateAuthoritativeSchedule(players, courts, options);

        ScheduledMatchNode r1m1 = matches.get(0);
        ScheduledMatchNode r1m2 = matches.get(1);

        // Seed 1 (P1) is in Match 1, Seed 2 (P2) is in Match 2
        boolean p1InM1 = "P1".equals(r1m1.getParticipantA().getId()) || "P1".equals(r1m1.getParticipantB().getId());
        boolean p2InM2 = "P2".equals(r1m2.getParticipantA().getId()) || "P2".equals(r1m2.getParticipantB().getId());

        assertTrue(p1InM1, "Seed 1 must be in top half (Match 1)");
        assertTrue(p2InM2, "Seed 2 must be in bottom half (Match 2)");
    }
}
