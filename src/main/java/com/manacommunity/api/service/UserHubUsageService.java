package com.manacommunity.api.service;

import java.util.List;
import java.util.Map;

public interface UserHubUsageService {
    void trackClick(Long userId, String hubId, String hubLabel);
    List<Map<String, Object>> getTopHubs(Long userId, int limit);
}
