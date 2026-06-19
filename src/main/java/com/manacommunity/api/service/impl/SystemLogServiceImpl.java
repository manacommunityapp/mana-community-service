package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.SystemLogResponse;
import com.manacommunity.api.dto.SystemStatsResponse;
import com.manacommunity.api.service.SystemLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SystemLogServiceImpl implements SystemLogService {

    @Value("${logging.file.name:logs/mana-service.log}")
    private String logFilePath;

    @Override
    public SystemLogResponse getLogTail(int lineCount, String levelFilter, String searchKeyword) {
        File logFile = new File(logFilePath);
        if (!logFile.exists() || !logFile.isFile()) {
            return SystemLogResponse.builder()
                    .lines(List.of("Log file not found: " + logFilePath))
                    .logFilePath(logFilePath)
                    .fileSizeKb(0)
                    .totalLinesReturned(0)
                    .build();
        }

        int requestedLines = Math.min(Math.max(lineCount, 10), 1000);
        // Read more lines than requested to account for filtering
        int readBuffer = (levelFilter != null || searchKeyword != null) ? requestedLines * 5 : requestedLines;

        List<String> tailLines = tailFile(logFile, readBuffer);

        // Apply filters
        List<String> filtered = tailLines;
        if (levelFilter != null && !levelFilter.isBlank()) {
            String lvl = levelFilter.toUpperCase().trim();
            filtered = filtered.stream()
                    .filter(line -> line.toUpperCase().contains(lvl))
                    .collect(Collectors.toList());
        }
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            String kw = searchKeyword.toLowerCase().trim();
            filtered = filtered.stream()
                    .filter(line -> line.toLowerCase().contains(kw))
                    .collect(Collectors.toList());
        }

        // Take last N after filtering
        if (filtered.size() > requestedLines) {
            filtered = filtered.subList(filtered.size() - requestedLines, filtered.size());
        }

        return SystemLogResponse.builder()
                .lines(filtered)
                .logFilePath(logFilePath)
                .fileSizeKb(logFile.length() / 1024)
                .totalLinesReturned(filtered.size())
                .build();
    }

    /**
     * Reads the last N lines from a file using reverse byte scanning.
     */
    private List<String> tailFile(File file, int lineCount) {
        List<String> lines = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) return lines;

            long pos = fileLength - 1;
            int count = 0;
            StringBuilder sb = new StringBuilder();

            // Skip trailing newline if present
            raf.seek(pos);
            if (raf.readByte() == '\n') pos--;

            while (pos >= 0 && count < lineCount) {
                raf.seek(pos);
                byte b = raf.readByte();
                if (b == '\n') {
                    lines.add(sb.reverse().toString());
                    sb = new StringBuilder();
                    count++;
                } else {
                    sb.append((char) b);
                }
                pos--;
            }
            if (sb.length() > 0) {
                lines.add(sb.reverse().toString());
            }
            Collections.reverse(lines);
        } catch (Exception e) {
            log.error("Failed to tail log file: {}", e.getMessage());
            lines.add("Error reading log file: " + e.getMessage());
        }
        return lines;
    }

    @Override
    public SystemStatsResponse getSystemStats() {
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        Runtime runtime = Runtime.getRuntime();

        double cpuLoad = osBean.getCpuLoad() * 100;
        long totalMemory = osBean.getTotalMemorySize() / (1024 * 1024);
        long freeMemory = osBean.getFreeMemorySize() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        double memPercent = totalMemory > 0 ? ((double) usedMemory / totalMemory) * 100 : 0;

        File root = new File("/");
        // On Windows, use the drive where the app runs
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            String userDir = System.getProperty("user.dir", "C:\\");
            root = new File(userDir.substring(0, 3)); // e.g. "D:\\"
        }
        long totalDisk = root.getTotalSpace() / (1024L * 1024 * 1024);
        long freeDisk = root.getUsableSpace() / (1024L * 1024 * 1024);
        long usedDisk = totalDisk - freeDisk;
        double diskPercent = totalDisk > 0 ? ((double) usedDisk / totalDisk) * 100 : 0;

        long jvmFree = runtime.freeMemory() / (1024 * 1024);
        long jvmTotal = runtime.totalMemory() / (1024 * 1024);
        long jvmMax = runtime.maxMemory() / (1024 * 1024);
        double jvmPercent = jvmMax > 0 ? ((double) (jvmTotal - jvmFree) / jvmMax) * 100 : 0;

        long uptimeMs = runtimeBean.getUptime();

        return SystemStatsResponse.builder()
                .cpuLoad(Math.round(cpuLoad * 10.0) / 10.0)
                .totalMemoryMb(totalMemory)
                .freeMemoryMb(freeMemory)
                .usedMemoryMb(usedMemory)
                .memoryUsagePercent(Math.round(memPercent * 10.0) / 10.0)
                .totalDiskGb(totalDisk)
                .freeDiskGb(freeDisk)
                .usedDiskGb(usedDisk)
                .diskUsagePercent(Math.round(diskPercent * 10.0) / 10.0)
                .jvmFreeMemoryMb(jvmFree)
                .jvmTotalMemoryMb(jvmTotal)
                .jvmMaxMemoryMb(jvmMax)
                .jvmUsagePercent(Math.round(jvmPercent * 10.0) / 10.0)
                .uptimeSeconds(uptimeMs / 1000)
                .activeThreads(Thread.activeCount())
                .build();
    }
}
