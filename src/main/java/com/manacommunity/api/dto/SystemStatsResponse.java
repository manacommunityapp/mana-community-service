package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemStatsResponse {
    private double cpuLoad;
    private long totalMemoryMb;
    private long freeMemoryMb;
    private long usedMemoryMb;
    private double memoryUsagePercent;
    private long totalDiskGb;
    private long freeDiskGb;
    private long usedDiskGb;
    private double diskUsagePercent;
    private long jvmFreeMemoryMb;
    private long jvmTotalMemoryMb;
    private long jvmMaxMemoryMb;
    private double jvmUsagePercent;
    private long uptimeSeconds;
    private int activeThreads;
}
