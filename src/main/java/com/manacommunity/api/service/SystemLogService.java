package com.manacommunity.api.service;

import com.manacommunity.api.dto.SystemLogResponse;
import com.manacommunity.api.dto.SystemStatsResponse;

public interface SystemLogService {
    SystemLogResponse getLogTail(int lineCount, String levelFilter, String searchKeyword);
    SystemStatsResponse getSystemStats();
}
