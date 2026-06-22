package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.SystemLogResponse;
import com.manacommunity.api.dto.SystemStatsResponse;
import com.manacommunity.api.service.SystemLogService;
import io.micrometer.core.instrument.MeterRegistry;
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
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

@Slf4j
@Service
public class SystemLogServiceImpl implements SystemLogService {

    /** Counts failures of the admin log-tail (the 500 seen on /api/admin/logs). */
    private final MeterRegistry meterRegistry;

    public SystemLogServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Value("${LOG_DIR:logs}")
    private String logDir;

    @Value("${logging.file.name:logs/mana-service.log}")
    private String appLogFilePath;

    @Value("${logging.frontend.file:${LOG_DIR:logs}/frontend.log}")
    private String frontendLogFilePath;

    @Value("${logging.database.file:${LOG_DIR:logs}/database.log}")
    private String dbLogFilePath;

    private static final java.util.Map<String, String> LOG_TYPE_TO_FILE = new java.util.LinkedHashMap<>();

    @PostConstruct
    public void init() {
        File logsDir = new File(logDir);
        if (!logsDir.exists()) {
            boolean created = logsDir.mkdirs();
            if (created) {
                log.info("Created log directory: {}", logsDir.getAbsolutePath());
            } else {
                log.error("Failed to create log directory: {}. Logs may not be written.", logsDir.getAbsolutePath());
            }
        }
        File archiveDir = new File(logsDir, "archive");
        if (!archiveDir.exists()) {
            archiveDir.mkdirs();
        }

        LOG_TYPE_TO_FILE.put("APPLICATION", appLogFilePath);
        LOG_TYPE_TO_FILE.put("ERROR", logDir + "/error.log");
        LOG_TYPE_TO_FILE.put("SECURITY", logDir + "/security.log");
        LOG_TYPE_TO_FILE.put("AUDIT", logDir + "/audit.log");
        LOG_TYPE_TO_FILE.put("FRONTEND", frontendLogFilePath);
        LOG_TYPE_TO_FILE.put("SCHEDULER", logDir + "/scheduler.log");
        LOG_TYPE_TO_FILE.put("AUCTION", logDir + "/auction.log");
        LOG_TYPE_TO_FILE.put("CHAT", logDir + "/chat.log");
        LOG_TYPE_TO_FILE.put("NOTIFICATION", logDir + "/notification.log");

        log.info("Log directory resolved to: {}", logsDir.getAbsolutePath());
    }

    private String resolveLogPath(String logType) {
        if (logType == null || logType.isBlank()) return appLogFilePath;
        String key = logType.toUpperCase().trim();
        return LOG_TYPE_TO_FILE.getOrDefault(key, appLogFilePath);
    }

    private String resolveLogTypeLabel(String logType) {
        if (logType == null || logType.isBlank()) return "APPLICATION";
        String key = logType.toUpperCase().trim();
        return LOG_TYPE_TO_FILE.containsKey(key) ? key : "APPLICATION";
    }

    private List<String> readAllLines(File file) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            // Read at most 10000 lines for memory safety
            while ((line = reader.readLine()) != null && count < 10000) {
                lines.add(line);
                count++;
            }
            if (reader.readLine() != null) {
                lines.add("... [Log truncated: file exceeds 10,000 lines limit for safety] ...");
            }
        } catch (Exception e) {
            log.error("Failed to read all lines from log file: {}", e.getMessage());
            lines.add("Error reading log file: " + e.getMessage());
        }
        return lines;
    }

    @Override
    public SystemLogResponse getLogTail(int lineCount, String levelFilter, String searchKeyword, String logType) {
        String resolvedType = resolveLogTypeLabel(logType);
        try {
            String path = resolveLogPath(logType);
            if (path == null || path.isBlank()) path = logDir + "/mana-service.log";

            File logFile = new File(path);
            if (!logFile.exists() || !logFile.isFile()) {
                return SystemLogResponse.builder()
                        .lines(List.of("Log file not found: " + path))
                        .logFilePath(path)
                        .fileSizeKb(0)
                        .totalLinesReturned(0)
                        .logType(resolvedType)
                        .build();
            }

            List<String> rawLines;
            if (lineCount <= 0) {
                rawLines = readAllLines(logFile);
            } else {
                int requestedLines = Math.min(Math.max(lineCount, 10), 1000);
                int readBuffer = (levelFilter != null || searchKeyword != null) ? requestedLines * 5 : requestedLines;
                rawLines = tailFile(logFile, readBuffer);
            }

            List<String> filtered = rawLines;
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

            if (lineCount > 0 && filtered.size() > lineCount) {
                filtered = filtered.subList(filtered.size() - lineCount, filtered.size());
            }

            return SystemLogResponse.builder()
                    .lines(filtered)
                    .logFilePath(path)
                    .fileSizeKb(logFile.length() / 1024)
                    .totalLinesReturned(filtered.size())
                    .logType(resolvedType)
                    .build();
        } catch (Exception e) {
            log.error("Error in getLogTail for type {}: ", resolvedType, e);
            // Metric: surface the /api/admin/logs failure so a Grafana alarm can fire
            // on it instead of it being discovered by hand. Tagged by log type and
            // exception class for quick triage.
            meterRegistry.counter("admin.logs.fetch.errors",
                    "logType", resolvedType,
                    "exception", e.getClass().getSimpleName()).increment();
            return SystemLogResponse.builder()
                    .lines(List.of("Internal error retrieving logs: " + e.getClass().getSimpleName() + " - " + e.getMessage()))
                    .logFilePath(resolveLogPath(logType))
                    .fileSizeKb(0)
                    .totalLinesReturned(0)
                    .logType(resolvedType)
                    .build();
        }
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
        try {
            java.lang.management.OperatingSystemMXBean baseOsBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = -1.0;
            long totalMemory = 0;
            long freeMemory = 0;

            if (baseOsBean instanceof com.sun.management.OperatingSystemMXBean osBean) {
                cpuLoad = osBean.getCpuLoad() * 100;
                totalMemory = osBean.getTotalMemorySize() / (1024 * 1024);
                freeMemory = osBean.getFreeMemorySize() / (1024 * 1024);
            } else {
                double loadAvg = baseOsBean.getSystemLoadAverage();
                if (loadAvg >= 0) {
                    cpuLoad = loadAvg * 100;
                }
            }

            long usedMemory = totalMemory - freeMemory;
            double memPercent = totalMemory > 0 ? ((double) usedMemory / totalMemory) * 100 : 0;

            File root = new File("/");
            String osName = System.getProperty("os.name", "").toLowerCase();
            if (osName.contains("win")) {
                String userDir = System.getProperty("user.dir", "C:\\");
                root = new File(userDir.substring(0, 3));
            }
            long totalDisk = root.getTotalSpace() / (1024L * 1024 * 1024);
            long freeDisk = root.getUsableSpace() / (1024L * 1024 * 1024);
            long usedDisk = totalDisk - freeDisk;
            double diskPercent = totalDisk > 0 ? ((double) usedDisk / totalDisk) * 100 : 0;

            Runtime runtime = Runtime.getRuntime();
            long jvmFree = runtime.freeMemory() / (1024 * 1024);
            long jvmTotal = runtime.totalMemory() / (1024 * 1024);
            long jvmMax = runtime.maxMemory() / (1024 * 1024);
            double jvmPercent = jvmMax > 0 ? ((double) (jvmTotal - jvmFree) / jvmMax) * 100 : 0;

            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            long uptimeMs = runtimeBean.getUptime();

            return SystemStatsResponse.builder()
                    .cpuLoad(cpuLoad >= 0 ? Math.round(cpuLoad * 10.0) / 10.0 : -1.0)
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
        } catch (Exception e) {
            log.error("Failed to gather system stats: ", e);
            return SystemStatsResponse.builder()
                    .cpuLoad(-1.0)
                    .uptimeSeconds(0)
                    .activeThreads(Thread.activeCount())
                    .build();
        }
    }

    @Override
    public List<String> getAvailableLogTypes() {
        return new ArrayList<>(LOG_TYPE_TO_FILE.keySet());
    }
}
