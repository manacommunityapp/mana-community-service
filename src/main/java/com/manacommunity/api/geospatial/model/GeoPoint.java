package com.manacommunity.api.geospatial.model;

/**
 * Immutable geographic coordinate point (WGS84).
 */
public record GeoPoint(double lat, double lon) {

    public GeoPoint {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees: " + lat);
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees: " + lon);
        }
    }
}
