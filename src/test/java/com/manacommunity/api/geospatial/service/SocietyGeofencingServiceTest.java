package com.manacommunity.api.geospatial.service;

import com.manacommunity.api.geospatial.engine.GeodesicDistanceCalculator;
import com.manacommunity.api.geospatial.engine.HierarchicalSpatialIndex;
import com.manacommunity.api.geospatial.engine.PointInPolygonRayCaster;
import com.manacommunity.api.geospatial.model.SocietyDetectionRequest;
import com.manacommunity.api.geospatial.model.SocietyDetectionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocietyGeofencingServiceTest {

    private SocietyGeofencingService geofencingService;

    @BeforeEach
    void setUp() {
        HierarchicalSpatialIndex spatialIndex = new HierarchicalSpatialIndex();
        PointInPolygonRayCaster rayCaster = new PointInPolygonRayCaster();
        GeodesicDistanceCalculator distanceCalculator = new GeodesicDistanceCalculator();
        geofencingService = new SocietyGeofencingService(spatialIndex, rayCaster, distanceCalculator);
        geofencingService.initReferenceGeofences();
    }

    @Test
    void testDetectInsideAparnaSarovarGrandeForDeliveryPartner() {
        // Point strictly inside Aparna Sarovar Grande (lat ~17.4725, lon ~78.3340)
        SocietyDetectionRequest request = new SocietyDetectionRequest(17.47250, 78.33400, "DELIVERY_PARTNER", 8.0);
        SocietyDetectionResponse response = geofencingService.detectSocietyAndNearestGate(request);

        assertTrue(response.detected());
        assertTrue(response.withinPerimeter());
        assertEquals(101L, response.communityId());
        assertEquals("Aparna Sarovar Grande", response.communityName());
        assertNotNull(response.nearestGate());
        assertTrue(response.nearestGate().allowsDelivery());
        assertTrue(response.executionLatencyMs() < 10.0, "Execution latency must be under 10ms");
    }

    @Test
    void testDetectInsideMyHomeBhoojaForResident() {
        // Point strictly inside My Home Bhooja (lat ~17.4360, lon ~78.3770)
        SocietyDetectionRequest request = new SocietyDetectionRequest(17.43600, 78.37700, "RESIDENT", 5.0);
        SocietyDetectionResponse response = geofencingService.detectSocietyAndNearestGate(request);

        assertTrue(response.detected());
        assertEquals(102L, response.communityId());
        assertEquals("My Home Bhooja", response.communityName());
        assertNotNull(response.nearestGate());
    }

    @Test
    void testOutsideAllBoundariesReturnsNotDetected() {
        // Coordinate in the Arabian Sea or outside supported boundaries
        SocietyDetectionRequest request = new SocietyDetectionRequest(15.00000, 75.00000, "DELIVERY_PARTNER", 20.0);
        SocietyDetectionResponse response = geofencingService.detectSocietyAndNearestGate(request);

        assertFalse(response.detected());
        assertFalse(response.withinPerimeter());
        assertNull(response.communityId());
    }

    @Test
    void testDeliveryRiderBypassesResidentOnlyGate() {
        // Coordinate close to Gate 3 (Resident RFID Gate: 17.47280, 78.33670) in Aparna Sarovar Grande
        SocietyDetectionRequest deliveryReq = new SocietyDetectionRequest(17.47280, 78.33660, "DELIVERY_PARTNER", 5.0);
        SocietyDetectionResponse deliveryResp = geofencingService.detectSocietyAndNearestGate(deliveryReq);

        assertTrue(deliveryResp.detected());
        assertNotEquals("RESIDENT_ONLY", deliveryResp.nearestGate().gateType(),
                "Delivery partners must not be routed to resident-only RFID barriers");
    }
}
