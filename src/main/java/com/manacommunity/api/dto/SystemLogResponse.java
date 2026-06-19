package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SystemLogResponse {
    private List<String> lines;
    private String logFilePath;
    private long fileSizeKb;
    private int totalLinesReturned;
}
