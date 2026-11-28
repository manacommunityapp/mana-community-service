package com.manacommunity.api.geospatial.service;

import com.manacommunity.api.geospatial.engine.GeodesicDistanceCalculator;
import com.manacommunity.api.geospatial.engine.HierarchicalSpatialIndex;
import com.manacommunity.api.geospatial.engine.PointInPolygonRayCaster;
import com.manacommunity.api.geospatial.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time geofencing service delivering sub-10ms auto-society detection and multi-gate routing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocietyGeofencingService {

    private final HierarchicalSpatialIndex spatialIndex;
    private final PointInPolygonRayCaster rayCaster;
    private final GeodesicDistanceCalculator distanceCalculator;

    // Registry of community gates by community ID
    private final Map<Long, List<CommunityGateDto>> communityGatesMap = new ConcurrentHashMap<>();
    private final Map<Long, GeoPolygon> communityPolygonMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void initReferenceGeofences() {
        seedAparnaSarovarGrande();
        seedMyHomeBhooja();
        seedPrestigeShantiniketan();
        log.info("Initialized {} reference gated communities into spatial grid across {} cells",
                communityPolygonMap.size(), spatialIndex.totalIndexedCells());
    }

    /**
     * Detect enclosing society and nearest permitted gate in under 10ms.
     */
    public SocietyDetectionResponse detectSocietyAndNearestGate(SocietyDetectionRequest request) {
        long startTime = System.nanoTime();
        double lat = request.latitude();
        double lon = request.longitude();
        String callerRole = request.callerRole();

        // Stage 1: Coarse Spatial Grid Lookup (O(1))
        List<GeoPolygon> candidates = spatialIndex.queryCandidates(lat, lon);

        GeoPolygon enclosingPolygon = null;
        boolean strictlyInside = false;

        // Stage 2 & 3: AABB Rejection & Jordan Curve Ray-Casting PIP
        for (GeoPolygon candidate : candidates) {
            if (rayCaster.isPointInside(lat, lon, candidate)) {
                enclosingPolygon = candidate;
                strictlyInside = true;
                break;
            }
        }

        // If not strictly inside, check if at society perimeter (< 60m buffer)
        boolean withinPerimeter = strictlyInside;
        if (enclosingPolygon == null) {
            for (GeoPolygon candidate : candidates) {
                double dist = rayCaster.distanceToBoundaryMeters(lat, lon, candidate);
                if (dist <= 60.0) {
                    enclosingPolygon = candidate;
                    withinPerimeter = true;
                    break;
                }
            }
        }

        double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;

        if (enclosingPolygon == null) {
            return SocietyDetectionResponse.notDetected(latencyMs);
        }

        Long communityId = enclosingPolygon.communityId();
        String communityName = enclosingPolygon.communityName();

        // Stage 4: Geodesic Nearest Gate Resolution with Access Policy Enforcement
        List<CommunityGateDto> gates = communityGatesMap.getOrDefault(communityId, Collections.emptyList());
        CommunityGateDto nearestGate = null;
        double minDistance = Double.MAX_VALUE;

        boolean isDelivery = "DELIVERY_PARTNER".equalsIgnoreCase(callerRole);

        for (CommunityGateDto gate : gates) {
            // Filter gates by access policy
            if (isDelivery && !gate.allowsDelivery()) {
                continue; // Skip resident-only RFID barriers for delivery riders
            }

            double d = distanceCalculator.calculateDistanceMeters(lat, lon, gate.latitude(), gate.longitude());
            if (d < minDistance) {
                minDistance = d;
                nearestGate = new CommunityGateDto(
                        gate.gateId(),
                        gate.gateCode(),
                        gate.gateName(),
                        gate.gateType(),
                        gate.latitude(),
                        gate.longitude(),
                        Math.round(d * 10.0) / 10.0,
                        gate.intercomExtension()
                );
            }
        }

        double confidence = strictlyInside ? 0.99 : 0.88;
        String advice = buildNavigationAdvice(callerRole, nearestGate);

        return new SocietyDetectionResponse(
                true,
                withinPerimeter,
                communityId,
                communityName,
                confidence,
                nearestGate,
                gates.size(),
                latencyMs,
                advice
        );
    }

    private String buildNavigationAdvice(String callerRole, CommunityGateDto gate) {
        if (gate == null) {
            return "Society boundary detected. Proceed to main security desk.";
        }
        if ("DELIVERY_PARTNER".equalsIgnoreCase(callerRole)) {
            return String.format("Delivery partner routed to %s (%s). Please park in designated two-wheeler bays and present gate pass.",
                    gate.gateName(), gate.gateCode());
        }
        if ("CAB_DRIVER".equalsIgnoreCase(callerRole)) {
            return String.format("Cab pickup/drop authorized through %s (%s). Speed limit inside premises is 20 km/h.",
                    gate.gateName(), gate.gateCode());
        }
        return String.format("Proceed through %s (%s). Security Intercom: Ext %s.",
                gate.gateName(), gate.gateCode(), gate.intercomExtension() != null ? gate.intercomExtension() : "1001");
    }

    public void registerCommunityBoundary(GeoPolygon polygon, List<CommunityGateDto> gates) {
        communityPolygonMap.put(polygon.communityId(), polygon);
        spatialIndex.indexPolygon(polygon);
        if (gates != null) {
            communityGatesMap.put(polygon.communityId(), new ArrayList<>(gates));
        }
    }

    // ──────────────────────────── Reference Seed Data ────────────────────────────

    private void seedAparnaSarovarGrande() {
        // 40-acre gated society in Nallagandla, Hyderabad
        List<GeoPoint> vertices = List.of(
                new GeoPoint(17.47400, 78.33200),
                new GeoPoint(17.47410, 78.33650),
                new GeoPoint(17.47150, 78.33680),
                new GeoPoint(17.47100, 78.33280),
                new GeoPoint(17.47250, 78.33180)
        );
        GeoPolygon polygon = GeoPolygon.of(101L, "Aparna Sarovar Grande", vertices);

        List<CommunityGateDto> gates = List.of(
                new CommunityGateDto(1L, "GATE-01", "Main North Entrance", "ALL_TRAFFIC", 17.47405, 78.33400, 0.0, "1001"),
                new CommunityGateDto(2L, "GATE-02", "South Delivery & Commercial Gate", "DELIVERY_ONLY", 17.47110, 78.33350, 0.0, "1002"),
                new CommunityGateDto(3L, "GATE-03", "East Resident FastTag RFID Barrier", "RESIDENT_ONLY", 17.47280, 78.33670, 0.0, "1003"),
                new CommunityGateDto(4L, "GATE-04", "West School Bus Bay", "ALL_TRAFFIC", 17.47320, 78.33190, 0.0, "1004"),
                new CommunityGateDto(5L, "GATE-05", "Service & Contractor Entry", "SERVICE", 17.47160, 78.33620, 0.0, "1005")
        );
        registerCommunityBoundary(polygon, gates);
    }

    private void seedMyHomeBhooja() {
        // 18-acre luxury community in Hitec City / Silpa Gram, Hyderabad
        List<GeoPoint> vertices = List.of(
                new GeoPoint(17.43800, 78.37500),
                new GeoPoint(17.43820, 78.37950),
                new GeoPoint(17.43450, 78.37920),
                new GeoPoint(17.43420, 78.37480)
        );
        GeoPolygon polygon = GeoPolygon.of(102L, "My Home Bhooja", vertices);

        List<CommunityGateDto> gates = List.of(
                new CommunityGateDto(6L, "GATE-01", "Boulevard Main Gate", "ALL_TRAFFIC", 17.43810, 78.37700, 0.0, "2001"),
                new CommunityGateDto(7L, "GATE-02", "Logistics & Delivery Depot", "DELIVERY_ONLY", 17.43440, 78.37600, 0.0, "2002"),
                new CommunityGateDto(8L, "GATE-03", "Resident Boom Barrier", "RESIDENT_ONLY", 17.43600, 78.37940, 0.0, "2003"),
                new CommunityGateDto(9L, "GATE-04", "Tower J Emergency Exit", "EMERGENCY_ONLY", 17.43500, 78.37490, 0.0, "2004")
        );
        registerCommunityBoundary(polygon, gates);
    }

    private void seedPrestigeShantiniketan() {
        // 105-acre mega township in Whitefield, Bengaluru
        List<GeoPoint> vertices = List.of(
                new GeoPoint(12.98800, 77.72500),
                new GeoPoint(12.98850, 77.73400),
                new GeoPoint(12.98100, 77.73350),
                new GeoPoint(12.98050, 77.72450)
        );
        GeoPolygon polygon = GeoPolygon.of(103L, "Prestige Shantiniketan", vertices);

        List<CommunityGateDto> gates = List.of(
                new CommunityGateDto(10L, "GATE-01", "South Main Arch Gate", "ALL_TRAFFIC", 12.98100, 77.72800, 0.0, "3001"),
                new CommunityGateDto(11L, "GATE-02", "E-Commerce Delivery Hub", "DELIVERY_ONLY", 12.98500, 77.73380, 0.0, "3002"),
                new CommunityGateDto(12L, "GATE-03", "Tech Park Commercial Link", "ALL_TRAFFIC", 12.98820, 77.73000, 0.0, "3003")
        );
        registerCommunityBoundary(polygon, gates);
    }
}
