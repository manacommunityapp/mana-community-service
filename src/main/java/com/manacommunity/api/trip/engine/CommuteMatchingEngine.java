package com.manacommunity.api.trip.engine;

import org.springframework.stereotype.Component;

@Component
public class CommuteMatchingEngine {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEFAULT_MAX_DETOUR_KM = 3.5;

    public double calculateDistanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return 0.0;
        }
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }

    public boolean isRouteCompatible(Double driverOriginLat, Double driverOriginLng,
                                     Double driverDestLat, Double driverDestLng,
                                     Double riderOriginLat, Double riderOriginLng,
                                     Double riderDestLat, Double riderDestLng,
                                     Double maxDetourKm) {
        if (driverOriginLat == null || driverDestLat == null || riderOriginLat == null || riderDestLat == null) {
            return true;
        }

        double maxDetour = maxDetourKm != null ? maxDetourKm : DEFAULT_MAX_DETOUR_KM;
        double pickupDeviation = calculateDistanceKm(driverOriginLat, driverOriginLng, riderOriginLat, riderOriginLng);
        double dropoffDeviation = calculateDistanceKm(driverDestLat, driverDestLng, riderDestLat, riderDestLng);

        return (pickupDeviation <= maxDetour) && (dropoffDeviation <= maxDetour);
    }

    public boolean canRiderBook(boolean isLadiesOnlyRide, String riderGender) {
        if (!isLadiesOnlyRide) {
            return true;
        }
        return "FEMALE".equalsIgnoreCase(riderGender);
    }
}
