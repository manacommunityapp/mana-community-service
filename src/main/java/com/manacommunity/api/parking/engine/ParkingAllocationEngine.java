package com.manacommunity.api.parking.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class ParkingAllocationEngine {

    public record SlotCandidate(Long slotId, String slotNumber, String slotType, String zone, String floor, boolean isOccupied) {}

    /**
     * Checks if a vehicle type is compatible with a parking slot type.
     */
    public boolean isSlotCompatible(String vehicleType, String slotType) {
        if (vehicleType == null || slotType == null) return false;
        String vType = vehicleType.toUpperCase();
        String sType = slotType.toUpperCase();

        if (vType.equals("TWO_WHEELER") || vType.equals("BIKE")) {
            return sType.contains("BIKE") || sType.contains("TWO_WHEELER") || sType.equals("OPEN");
        }
        if (vType.equals("EV") || vType.equals("ELECTRIC_CAR")) {
            return sType.contains("EV") || sType.equals("COVERED");
        }
        return sType.equals("COVERED") || sType.equals("OPEN") || sType.equals("STANDARD");
    }

    /**
     * Finds the best matching available slot for a vehicle given preference zone.
     */
    public SlotCandidate findBestSlot(List<SlotCandidate> candidates, String vehicleType, String preferredZone) {
        if (candidates == null || candidates.isEmpty()) return null;

        return candidates.stream()
                .filter(s -> !s.isOccupied())
                .filter(s -> isSlotCompatible(vehicleType, s.slotType()))
                .sorted((a, b) -> {
                    boolean aPref = preferredZone != null && preferredZone.equalsIgnoreCase(a.zone());
                    boolean bPref = preferredZone != null && preferredZone.equalsIgnoreCase(b.zone());
                    if (aPref && !bPref) return -1;
                    if (!aPref && bPref) return 1;
                    return a.slotNumber().compareTo(b.slotNumber());
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Computes parking occupancy and utilization rate percentage.
     */
    public double calculateOccupancyRate(int totalSlots, int occupiedSlots) {
        if (totalSlots <= 0) return 0.0;
        double rate = ((double) occupiedSlots / (double) totalSlots) * 100.0;
        return Math.round(rate * 100.0) / 100.0;
    }
}
