package com.manacommunity.api.geospatial.engine;

import org.springframework.stereotype.Component;

/**
 * Geodesic distance and navigation calculations using the spherical Haversine formula.
 */
@Component
public class GeodesicDistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    /**
     * Compute Great-Circle distance in meters between two WGS84 geographic points.
     */
    public double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Compute navigation bearing in degrees (0° North, 90° East, 180° South, 270° West).
     */
    public double calculateBearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);

        double bearingRad = Math.atan2(y, x);
        return (Math.toDegrees(bearingRad) + 360.0) % 360.0;
    }
}
