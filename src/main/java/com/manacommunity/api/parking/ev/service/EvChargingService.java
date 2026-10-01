package com.manacommunity.api.parking.ev.service;

import com.manacommunity.api.parking.ev.dto.*;
import java.util.List;

public interface EvChargingService {
    EvStationResponse registerStation(EvStationResponse request);
    List<EvStationResponse> getCommunityStations(Long communityId);
    EvStationResponse getStationById(Long stationId);
    EvLiveTelemetryResponse getLiveTelemetry(Long stationId);
    
    EvSessionResponse startSession(Long residentId, Long communityId, EvStartSessionRequest request);
    EvSessionResponse stopSession(Long sessionId, Long residentId, EvStopSessionRequest request);
    
    void processTelemetry(EvTelemetryRequest request);
    
    List<EvSessionResponse> getResidentSessions(Long residentId);
    List<EvSessionResponse> getCommunitySessions(Long communityId);
    EvSessionResponse getSessionById(Long sessionId);
}
