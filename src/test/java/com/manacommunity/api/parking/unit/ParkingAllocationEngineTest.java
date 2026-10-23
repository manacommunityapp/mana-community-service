package com.manacommunity.api.parking.unit;

import com.manacommunity.api.parking.engine.ParkingAllocationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Parking Allocation Engine Unit Tests")
class ParkingAllocationEngineTest {

    private ParkingAllocationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ParkingAllocationEngine();
    }

    @Test
    @DisplayName("Should correctly validate vehicle to slot compatibility")
    void shouldValidateCompatibility() {
        assertTrue(engine.isSlotCompatible("CAR", "COVERED"));
        assertTrue(engine.isSlotCompatible("CAR", "OPEN"));
        assertTrue(engine.isSlotCompatible("BIKE", "TWO_WHEELER"));
        assertTrue(engine.isSlotCompatible("EV", "EV_CHARGING"));
        assertFalse(engine.isSlotCompatible("CAR", "TWO_WHEELER"));
    }

    @Test
    @DisplayName("Should find best available slot prioritizing preferred zone")
    void shouldFindBestSlot() {
        var s1 = new ParkingAllocationEngine.SlotCandidate(1L, "A-101", "COVERED", "North", "B1", false);
        var s2 = new ParkingAllocationEngine.SlotCandidate(2L, "B-201", "COVERED", "South", "B1", false);
        var s3 = new ParkingAllocationEngine.SlotCandidate(3L, "B-202", "COVERED", "South", "B1", true); // occupied

        var best = engine.findBestSlot(List.of(s1, s2, s3), "CAR", "South");
        assertNotNull(best);
        assertEquals("B-201", best.slotNumber());
    }

    @Test
    @DisplayName("Should calculate occupancy rate accurately")
    void shouldCalculateOccupancyRate() {
        double rate = engine.calculateOccupancyRate(100, 75);
        assertEquals(75.0, rate);
        assertEquals(0.0, engine.calculateOccupancyRate(0, 0));
    }
}
