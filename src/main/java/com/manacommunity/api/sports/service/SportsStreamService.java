package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.*;

public interface SportsStreamService {
    SportsStreamResponse getStream(Long matchId);
    SportsStreamResponse startStream(Long matchId, String title);
    SportsStreamResponse endStream(Long matchId);
    SportsStreamResponse addCamera(Long matchId, SportsStreamCameraRequest request);
    SportsStreamResponse removeCamera(Long matchId, Long cameraId);
    SportsStreamResponse switchActiveCamera(Long matchId, Long cameraId);
    SportsStreamResponse postMessage(Long matchId, SportsStreamChatRequest request, String authorName);
}
