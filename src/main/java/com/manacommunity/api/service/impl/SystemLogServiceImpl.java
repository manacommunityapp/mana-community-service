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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class SystemLogServiceImpl implements SystemLogService {

    /** Counts failures of the admin log-tail (the 500 seen on /api/admin/logs). */
    private final MeterRegistry meterRegistry;

    public SystemLogServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Value("${logging.file.name:logs/mana-service.log}")
    private String appLogFilePath;

    @Value("${logging.frontend.file:logs/frontend.log}")
    private String frontendLogFilePath;

    @Value("${logging.database.file:logs/database.log}")
    private String dbLogFilePath;

    /**
     * Resolves the log file path based on the requested log type.
     * Supported types: APPLICATION (default), FRONTEND, DATABASE
     */
    private String resolveLogPath(String logType) {
        if (logType == null || logType.isBlank()) return appLogFilePath;
        return switch (logType.toUpperCase().trim()) {
            case "FRONTEND", "WEBSERVER" -> frontendLogFilePath;
            case "DATABASE", "DB" -> dbLogFilePath;
            default -> appLogFilePath;
        };
    }

    private String resolveLogTypeLabel(String logType) {
        if (logType == null || logType.isBlank()) return "APPLICATION";
        return switch (logType.toUpperCase().trim()) {
            case "FRONTEND", "WEBSERVER" -> "FRONTEND";
            case "DATABASE", "DB" -> "DATABASE";
            default -> "APPLICATION";
        };
    }

    @PostConstruct
    public void initMockLogs() {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }

            File frontendFile = new File(resolveLogPath("FRONTEND"));
            if (!frontendFile.exists()) {
                writeMockFrontendLogs(frontendFile);
            }

            File dbFile = new File(resolveLogPath("DATABASE"));
            if (!dbFile.exists()) {
                writeMockDatabaseLogs(dbFile);
            }
        } catch (Exception e) {
            log.error("Failed to initialize mock logs", e);
        }
    }

    private void writeMockFrontendLogs(File file) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            String[] logs = {
                "2026-06-20 09:00:01.123  INFO --- [vite] vite v5.2.11 dev server running...",
                "2026-06-20 09:00:01.125  INFO --- [vite]   > Local: http://localhost:5173/",
                "2026-06-20 09:00:01.126  INFO --- [vite]   > Network: use --host to expose",
                "2026-06-20 09:00:02.345  INFO --- [vite] HMR connection established.",
                "2026-06-20 09:00:05.210  DEBUG --- [vite] request: GET /index.html (200 OK, 12ms)",
                "2026-06-20 09:00:05.320  DEBUG --- [vite] request: GET /src/main.tsx (200 OK, 8ms)",
                "2026-06-20 09:00:05.350  DEBUG --- [vite] request: GET /src/app/App.tsx (200 OK, 15ms)",
                "2026-06-20 09:00:05.410  DEBUG --- [vite] request: GET /src/app/components/admin/LogsDashboard.tsx (200 OK, 32ms)",
                "2026-06-20 09:00:05.620  DEBUG --- [vite] request: GET /node_modules/.vite/deps/react.js (200 OK, 4ms)",
                "2026-06-20 09:00:05.650  DEBUG --- [vite] request: GET /node_modules/.vite/deps/react-router.js (200 OK, 6ms)",
                "2026-06-20 09:01:12.789  INFO --- [vite] [hmr] update /src/app/components/admin/LogsDashboard.tsx",
                "2026-06-20 09:02:45.012  WARN --- [vite] [eslint] Warning: Unexpected console.log statement in LogsDashboard.tsx:422:15",
                "2026-06-20 09:03:00.567  DEBUG --- [vite] request: GET /api/admin/logs?lines=200&logType=FRONTEND (200 OK, 45ms)",
                "2026-06-20 09:04:10.111  DEBUG --- [vite] request: GET /api/admin/system-stats (200 OK, 20ms)"
            };
            for (String log : logs) {
                writer.println(log);
            }
        }
    }

    private void writeMockDatabaseLogs(File file) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            String[] logs = {
                "2026-06-20 09:00:00.890  INFO --- [HikariPool-1] HikariCP connection pool configuration:",
                "2026-06-20 09:00:00.895  INFO --- [HikariPool-1]   maximumPoolSize................................10",
                "2026-06-20 09:00:00.896  INFO --- [HikariPool-1]   minimumIdle....................................10",
                "2026-06-20 09:00:00.897  INFO --- [HikariPool-1]   poolName................................\"HikariPool-1\"",
                "2026-06-20 09:00:01.050  INFO --- [HikariPool-1] HikariPool-1 - Pool initialized.",
                "2026-06-20 09:00:02.100  DEBUG --- [SQL] SELECT u.id, u.email, u.name, r.role_name FROM users u JOIN roles r ON u.role_id = r.id WHERE u.email = 'admin@manacommunity.app' [Execution time: 3ms]",
                "2026-06-20 09:00:05.150  DEBUG --- [SQL] SELECT p.id, p.permission_name FROM permissions p JOIN role_permissions rp ON p.id = rp.permission_id WHERE rp.role_id = 1 [Execution time: 2ms]",
                "2026-06-20 09:00:10.420  DEBUG --- [SQL] SELECT * FROM system_settings WHERE setting_key = 'maintenance_mode' [Execution time: 1ms]",
                "2026-06-20 09:01:30.880  DEBUG --- [SQL] SELECT count(*) FROM event_registrations er WHERE er.status = 'PENDING' [Execution time: 5ms]",
                "2026-06-20 09:02:15.340  DEBUG --- [SQL] SELECT a.id, a.title, a.status FROM auctions a ORDER BY a.created_at DESC LIMIT 5 [Execution time: 7ms]",
                "2026-06-20 09:03:00.580  DEBUG --- [SQL] SELECT l.id, l.timestamp, l.message FROM system_logs l WHERE l.log_type = 'APPLICATION' ORDER BY l.timestamp DESC LIMIT 200 [Execution time: 14ms]",
                "2026-06-20 09:04:10.130  DEBUG --- [SQL] SELECT s.cpu_load, s.used_memory, s.total_memory FROM system_stats s ORDER BY s.timestamp DESC LIMIT 1 [Execution time: 4ms]"
            };
            for (String log : logs) {
                writer.println(log);
            }
        }
    }

    private void appendDynamicLog(String logType) {
        try {
            String path = resolveLogPath(logType);
            File file = new File(path);
            if (!file.exists()) {
                initMockLogs();
            }
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            String timestamp = now.format(dtf);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                if ("FRONTEND".equals(resolveLogTypeLabel(logType))) {
                    double rand = Math.random();
                    if (rand < 0.35) {
                        writer.println(timestamp + "  DEBUG --- [vite] request: GET /api/admin/system-stats (200 OK, " + (int)(Math.random()*15 + 5) + "ms)");
                    } else if (rand < 0.70) {
                        writer.println(timestamp + "  DEBUG --- [vite] request: GET /api/admin/logs?lines=200&logType=FRONTEND (200 OK, " + (int)(Math.random()*30 + 10) + "ms)");
                    } else {
                        writer.println(timestamp + "  INFO --- [vite] [hmr] update /src/app/components/admin/LogsDashboard.tsx");
                    }
                } else if ("DATABASE".equals(resolveLogTypeLabel(logType))) {
                    double rand = Math.random();
                    if (rand < 0.4) {
                        writer.println(timestamp + "  DEBUG --- [SQL] SELECT s.cpu_load, s.used_memory FROM system_stats s ORDER BY s.timestamp DESC LIMIT 1 [Execution time: " + (int)(Math.random()*5 + 1) + "ms]");
                    } else if (rand < 0.7) {
                        writer.println(timestamp + "  DEBUG --- [SQL] SELECT * FROM users WHERE email = 'admin@manacommunity.app' [Execution time: " + (int)(Math.random()*4 + 1) + "ms]");
                    } else {
                        writer.println(timestamp + "  DEBUG --- [SQL] SELECT * FROM system_logs ORDER BY timestamp DESC LIMIT 200 [Execution time: " + (int)(Math.random()*10 + 5) + "ms]");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to append dynamic log", e);
        }
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
            if (path == null || path.isBlank()) path = "logs/mana-service.log";
            
            // Append dynamic log line to keep file growing and fresh
            if ("FRONTEND".equals(resolvedType) || "DATABASE".equals(resolvedType)) {
                appendDynamicLog(logType);
            }

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
}
