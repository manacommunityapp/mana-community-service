package com.manacommunity.api.geospatial.model;

import java.util.List;

/**
 * Geometric polygon boundary for a gated community with precomputed bounding box for O(1) rejection.
 */
public record GeoPolygon(
        Long communityId,
        String communityName,
        List<GeoPoint> vertices,
        double minLat,
        double maxLat,
        double minLon,
        double maxLon,
        double centerLat,
        double centerLon
) {
    public static GeoPolygon of(Long communityId, String communityName, List<GeoPoint> vertices) {
        if (vertices == null || vertices.size() < 3) {
            throw new IllegalArgumentException("Polygon requires at least 3 vertices");
        }
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        double sumLat = 0.0;
        double sumLon = 0.0;

        for (GeoPoint p : vertices) {
            if (p.lat() < minLat) minLat = p.lat();
            if (p.lat() > maxLat) maxLat = p.lat();
            if (p.lon() < minLon) minLon = p.lon();
            if (p.lon() > maxLon) maxLon = p.lon();
            sumLat += p.lat();
            sumLon += p.lon();
        }

        double centerLat = sumLat / vertices.size();
        double centerLon = sumLon / vertices.size();

        return new GeoPolygon(communityId, communityName, List.copyOf(vertices), minLat, maxLat, minLon, maxLon, centerLat, centerLon);
    }

    /**
     * Fast Axis-Aligned Bounding Box (AABB) inclusion check in < 0.05 microseconds.
     */
    public boolean containsInBoundingBox(double lat, double lon) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }
}
