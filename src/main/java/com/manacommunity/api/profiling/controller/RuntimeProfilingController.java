package com.manacommunity.api.profiling.controller;

import com.manacommunity.api.profiling.entity.*;
import com.manacommunity.api.profiling.entity.BrowserMemorySnapshot.*;
import com.manacommunity.api.profiling.entity.EventLoopSnapshot.*;
import com.manacommunity.api.profiling.entity.GcEvent.*;
import com.manacommunity.api.profiling.entity.ProfilingRecommendation.*;
import com.manacommunity.api.profiling.entity.ThreadPoolSnapshot.*;
import com.manacommunity.api.profiling.repository.*;
import com.manacommunity.api.profiling.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/runtime-profiling")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RuntimeProfilingController {

    private final JvmProfilingService jvmService;
    private final NodejsProfilingService nodejsService;
    private final BrowserProfilingService browserService;
    private final GcEventRepository gcRepo;
    private final ThreadPoolSnapshotRepository threadRepo;
    private final EventLoopSnapshotRepository eventLoopRepo;
    private final BrowserMemorySnapshotRepository browserRepo;
    private final ProfilingRecommendationRepository recsRepo;

    @GetMapping("/jvm/dashboard")
    public ResponseEntity<?> getJvmDashboard() {
        return ResponseEntity.ok(jvmService.getDashboard());
    }

    @GetMapping("/nodejs/dashboard")
    public ResponseEntity<?> getNodejsDashboard() {
        return ResponseEntity.ok(nodejsService.getDashboard());
    }

    @GetMapping("/browser/dashboard")
    public ResponseEntity<?> getBrowserDashboard() {
        return ResponseEntity.ok(browserService.getDashboard());
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations() {
        return ResponseEntity.ok(recsRepo.findAll());
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<?> seedDemoData() {
        LocalDateTime now = LocalDateTime.now();

        // =====================================================
        // JVM GC EVENTS — Gate Routing microservice morning peak
        // =====================================================
        List<GcEvent> gcEvents = new ArrayList<>();

        // Normal minor GC events (G1GC)
        for (int i = 0; i < 8; i++) {
            gcEvents.add(GcEvent.builder()
                    .serviceId("SVC-GATE-ROUTING").serviceName("Gate Routing Service").jvmVersion("Java 17.0.9")
                    .collector(GcCollector.G1GC).cause(GcCause.G1_EVACUATION_PAUSE)
                    .pauseMillis(15 + (long)(Math.random() * 25)).stopTheWorld(true)
                    .heapBeforeMb(2800 + (long)(Math.random() * 400)).heapAfterMb(1200 + (long)(Math.random() * 300))
                    .heapMaxMb(4096).edenUsedMb(800).survivorUsedMb(120).oldGenUsedMb(1600).metaspaceUsedMb(180)
                    .gcThreadCount(8).allocationRateMbPerSec(250).promotionRateMbPerSec(45)
                    .eventTimestamp(now.minusMinutes(60 - i * 7))
                    .gcLogSnippet("[GC pause (G1 Evacuation Pause) " + (15 + i * 3) + "ms]").build());
        }

        // THE BAD ONES — 600ms STW pauses during FCM spike
        gcEvents.add(GcEvent.builder()
                .serviceId("SVC-GATE-ROUTING").serviceName("Gate Routing Service").jvmVersion("Java 17.0.9")
                .collector(GcCollector.G1GC).cause(GcCause.ALLOCATION_FAILURE)
                .pauseMillis(612).stopTheWorld(true)
                .heapBeforeMb(3890).heapAfterMb(3200).heapMaxMb(4096)
                .edenUsedMb(1200).survivorUsedMb(400).oldGenUsedMb(2800).metaspaceUsedMb(195)
                .gcThreadCount(8).allocationRateMbPerSec(680).promotionRateMbPerSec(220)
                .eventTimestamp(now.minusMinutes(15))
                .gcLogSnippet("[Full GC (Allocation Failure) 3890M->3200M(4096M), 0.6120000 secs]").build());

        gcEvents.add(GcEvent.builder()
                .serviceId("SVC-GATE-ROUTING").serviceName("Gate Routing Service").jvmVersion("Java 17.0.9")
                .collector(GcCollector.G1GC).cause(GcCause.G1_HUMONGOUS_ALLOCATION)
                .pauseMillis(584).stopTheWorld(true)
                .heapBeforeMb(3750).heapAfterMb(3100).heapMaxMb(4096)
                .edenUsedMb(1100).survivorUsedMb(380).oldGenUsedMb(2700).metaspaceUsedMb(195)
                .gcThreadCount(8).allocationRateMbPerSec(720).promotionRateMbPerSec(195)
                .eventTimestamp(now.minusMinutes(12))
                .gcLogSnippet("[Full GC (G1 Humongous Allocation) 3750M->3100M(4096M), 0.5840000 secs]").build());

        gcEvents.add(GcEvent.builder()
                .serviceId("SVC-GATE-ROUTING").serviceName("Gate Routing Service").jvmVersion("Java 17.0.9")
                .collector(GcCollector.G1GC).cause(GcCause.ALLOCATION_FAILURE)
                .pauseMillis(645).stopTheWorld(true)
                .heapBeforeMb(3920).heapAfterMb(3300).heapMaxMb(4096)
                .edenUsedMb(1300).survivorUsedMb(420).oldGenUsedMb(2900).metaspaceUsedMb(198)
                .gcThreadCount(8).allocationRateMbPerSec(750).promotionRateMbPerSec(240)
                .eventTimestamp(now.minusMinutes(8))
                .gcLogSnippet("[Full GC (Allocation Failure) 3920M->3300M(4096M), 0.6450000 secs]").build());

        // ZGC comparison on notification service
        gcEvents.add(GcEvent.builder()
                .serviceId("SVC-NOTIFICATION").serviceName("Notification Service").jvmVersion("Java 21.0.1")
                .collector(GcCollector.ZGC).cause(GcCause.PROACTIVE)
                .pauseMillis(2).stopTheWorld(false)
                .heapBeforeMb(3200).heapAfterMb(1800).heapMaxMb(8192)
                .edenUsedMb(0).survivorUsedMb(0).oldGenUsedMb(1800).metaspaceUsedMb(210)
                .gcThreadCount(4).allocationRateMbPerSec(400).promotionRateMbPerSec(0)
                .eventTimestamp(now.minusMinutes(5))
                .gcLogSnippet("[ZGC Garbage Collection (Proactive) 3200M->1800M(8192M) 2.100ms]").build());

        gcRepo.saveAll(gcEvents);

        // =====================================================
        // THREAD POOL SNAPSHOTS
        // =====================================================
        List<ThreadPoolSnapshot> threadSnapshots = List.of(
                ThreadPoolSnapshot.builder()
                        .serviceId("SVC-GATE-ROUTING").serviceName("Gate Routing Service").poolName("http-nio-8080")
                        .poolType(PoolType.TOMCAT_HTTP).corePoolSize(200).maxPoolSize(200)
                        .activeThreads(198).queuedTasks(847).queueCapacity(100).completedTasks(1250000)
                        .rejectedTasks(234).cpuUsagePercent(94.5).avgTaskDurationMs(1200).p99TaskDurationMs(4500)
                        .health(PoolHealth.STARVED).virtualThreadsEnabled(false).virtualThreadCount(0)
                        .snapshotTimestamp(now.minusMinutes(10))
                        .threadDumpSnippet("198/200 threads BLOCKED on FCM HTTP call. Queue: 847 pending.").build(),
                ThreadPoolSnapshot.builder()
                        .serviceId("SVC-GATE-ROUTING").serviceName("Gate Routing Service").poolName("async-executor")
                        .poolType(PoolType.CUSTOM).corePoolSize(50).maxPoolSize(100)
                        .activeThreads(100).queuedTasks(2340).queueCapacity(5000).completedTasks(89000)
                        .rejectedTasks(0).cpuUsagePercent(78.3).avgTaskDurationMs(800).p99TaskDurationMs(3200)
                        .health(PoolHealth.WARNING).virtualThreadsEnabled(false).virtualThreadCount(0)
                        .snapshotTimestamp(now.minusMinutes(10))
                        .threadDumpSnippet("All 100 threads active. Queue growing at 500/sec.").build(),
                ThreadPoolSnapshot.builder()
                        .serviceId("SVC-NOTIFICATION").serviceName("Notification Service").poolName("virtual-thread-pool")
                        .poolType(PoolType.VIRTUAL_THREAD).corePoolSize(0).maxPoolSize(0)
                        .activeThreads(1250).queuedTasks(0).queueCapacity(0).completedTasks(5600000)
                        .rejectedTasks(0).cpuUsagePercent(45.2).avgTaskDurationMs(80).p99TaskDurationMs(350)
                        .health(PoolHealth.HEALTHY).virtualThreadsEnabled(true).virtualThreadCount(1250)
                        .snapshotTimestamp(now.minusMinutes(5))
                        .threadDumpSnippet("1250 virtual threads active. No carrier thread pinning detected.").build(),
                ThreadPoolSnapshot.builder()
                        .serviceId("SVC-GATE-ROUTING").serviceName("Gate Routing Service").poolName("scheduler-pool")
                        .poolType(PoolType.SCHEDULED).corePoolSize(10).maxPoolSize(10)
                        .activeThreads(3).queuedTasks(2).queueCapacity(1000).completedTasks(45000)
                        .rejectedTasks(0).cpuUsagePercent(12.0).avgTaskDurationMs(50).p99TaskDurationMs(200)
                        .health(PoolHealth.HEALTHY).virtualThreadsEnabled(false).virtualThreadCount(0)
                        .snapshotTimestamp(now.minusMinutes(10))
                        .threadDumpSnippet("3 active scheduled tasks. Normal operation.").build()
        );
        threadRepo.saveAll(threadSnapshots);

        // =====================================================
        // NODE.JS EVENT LOOP — ERP microservice
        // =====================================================
        List<EventLoopSnapshot> eventLoopSnapshots = new ArrayList<>();

        // Normal operation
        for (int i = 0; i < 5; i++) {
            eventLoopSnapshots.add(EventLoopSnapshot.builder()
                    .serviceId("SVC-ERP-NODE").serviceName("ERP Ledger Service").nodeVersion("v20.11.0")
                    .eventLoopLagMs(1.2 + Math.random() * 3).eventLoopLagP99Ms(8.5)
                    .eventLoopUtilizationPercent(35 + (long)(Math.random() * 15))
                    .activeHandles(45).activeRequests(12)
                    .heapUsedMb(180 + (long)(Math.random() * 40)).heapTotalMb(512).externalMb(25).arrayBuffersMb(8).rssMemoryMb(380)
                    .blockingCause(BlockingCause.NONE).blockingDurationMs(0)
                    .workerThreadsActive(0).workerThreadsIdle(4).jobQueueDepth(0)
                    .health(LoopHealth.HEALTHY).snapshotTimestamp(now.minusHours(2).plusMinutes(i * 20)).build());
        }

        // PDF export blocks the event loop
        eventLoopSnapshots.add(EventLoopSnapshot.builder()
                .serviceId("SVC-ERP-NODE").serviceName("ERP Ledger Service").nodeVersion("v20.11.0")
                .eventLoopLagMs(4200).eventLoopLagP99Ms(4200).eventLoopUtilizationPercent(100)
                .activeHandles(89).activeRequests(67)
                .heapUsedMb(890).heapTotalMb(1024).externalMb(120).arrayBuffersMb(45).rssMemoryMb(1200)
                .blockingCause(BlockingCause.PDF_GENERATION).blockingDurationMs(4200)
                .blockingStackTrace("at PDFDocument.render (node_modules/pdfkit/lib/document.js:234)\n  at generateLedgerPDF (src/services/ledger.js:89)\n  at /src/routes/export.js:45\n  — synchronous render of 12,000-row ledger PDF")
                .workerThreadsActive(0).workerThreadsIdle(4).jobQueueDepth(0)
                .health(LoopHealth.BLOCKED).snapshotTimestamp(now.minusMinutes(45)).build());

        // CSV export blocks the loop
        eventLoopSnapshots.add(EventLoopSnapshot.builder()
                .serviceId("SVC-ERP-NODE").serviceName("ERP Ledger Service").nodeVersion("v20.11.0")
                .eventLoopLagMs(2800).eventLoopLagP99Ms(2800).eventLoopUtilizationPercent(100)
                .activeHandles(72).activeRequests(54)
                .heapUsedMb(650).heapTotalMb(1024).externalMb(80).arrayBuffersMb(30).rssMemoryMb(900)
                .blockingCause(BlockingCause.CSV_EXPORT).blockingDurationMs(2800)
                .blockingStackTrace("at stringify (node_modules/csv-stringify/lib/sync.js:12)\n  at exportSocietyLedger (src/services/export.js:156)\n  — synchronous CSV generation for 50,000 transactions")
                .workerThreadsActive(0).workerThreadsIdle(4).jobQueueDepth(0)
                .health(LoopHealth.BLOCKED).snapshotTimestamp(now.minusMinutes(30)).build());

        // After fix: Worker Threads offloading
        eventLoopSnapshots.add(EventLoopSnapshot.builder()
                .serviceId("SVC-ERP-NODE").serviceName("ERP Ledger Service").nodeVersion("v20.11.0")
                .eventLoopLagMs(3.5).eventLoopLagP99Ms(12.0).eventLoopUtilizationPercent(42)
                .activeHandles(50).activeRequests(15)
                .heapUsedMb(220).heapTotalMb(512).externalMb(30).arrayBuffersMb(10).rssMemoryMb(450)
                .blockingCause(BlockingCause.NONE).blockingDurationMs(0)
                .workerThreadsActive(2).workerThreadsIdle(2).jobQueueDepth(3)
                .health(LoopHealth.HEALTHY).snapshotTimestamp(now.minusMinutes(5)).build());

        eventLoopRepo.saveAll(eventLoopSnapshots);

        // =====================================================
        // BROWSER MEMORY — Admin Portal 10-hour session
        // =====================================================
        List<BrowserMemorySnapshot> browserSnapshots = new ArrayList<>();

        // Estate Manager session — memory growing over 10 hours
        int[] hours = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        long[] heaps = {150, 220, 340, 480, 650, 870, 1100, 1450, 1800, 2100, 2400};
        long[] domNodes = {1200, 1800, 3200, 5500, 8200, 12000, 16500, 22000, 28000, 35000, 42000};
        long[] detached = {0, 50, 180, 420, 800, 1500, 2800, 4200, 6500, 9000, 12000};
        long[] listeners = {80, 120, 240, 450, 720, 1100, 1600, 2200, 2900, 3800, 4500};
        long[] wsMsgs = {0, 0, 50, 150, 400, 800, 1500, 2800, 4200, 6000, 8500};
        long[] queryCache = {20, 45, 90, 160, 280, 450, 700, 1000, 1400, 1800, 2200};
        MemoryHealth[] healths = {MemoryHealth.HEALTHY, MemoryHealth.HEALTHY, MemoryHealth.GROWING,
                MemoryHealth.GROWING, MemoryHealth.GROWING, MemoryHealth.LEAKING, MemoryHealth.LEAKING,
                MemoryHealth.LEAKING, MemoryHealth.CRITICAL, MemoryHealth.CRITICAL, MemoryHealth.CRITICAL};
        LeakSource[] leaks = {LeakSource.NONE, LeakSource.NONE, LeakSource.DETACHED_DOM,
                LeakSource.DETACHED_DOM, LeakSource.UNCLEANED_WEBSOCKET, LeakSource.UNCLEANED_WEBSOCKET,
                LeakSource.UNBOUNDED_QUERY_CACHE, LeakSource.EVENT_LISTENER_ACCUMULATION,
                LeakSource.EVENT_LISTENER_ACCUMULATION, LeakSource.DETACHED_DOM, LeakSource.DETACHED_DOM};

        for (int i = 0; i < hours.length; i++) {
            browserSnapshots.add(BrowserMemorySnapshot.builder()
                    .portalName("Admin Web Portal").userId("estate-mgr-001").browserVersion("Chrome 120.0.6099.130")
                    .sessionId("SESSION-ADMIN-001")
                    .jsHeapUsedMb(heaps[i]).jsHeapTotalMb(heaps[i] + 200).jsHeapLimitMb(4096)
                    .domNodeCount(domNodes[i]).detachedDomNodes(detached[i]).eventListenerCount(listeners[i])
                    .websocketConnections(3).websocketMessagesQueued(wsMsgs[i])
                    .websocketListenersCleaned(i < 3)
                    .reactQueryCacheEntries(queryCache[i]).reactQueryStaleCacheEntries(queryCache[i] / 2)
                    .reactComponentMountCount(500L + i * 200).reactComponentUnmountMissing(i * 15L)
                    .primaryLeakSource(leaks[i])
                    .sessionDurationMinutes(hours[i] * 60L)
                    .memoryGrowthRateMbPerHour(i == 0 ? 0 : (heaps[i] - heaps[i - 1]))
                    .health(healths[i])
                    .snapshotTimestamp(now.minusHours(10).plusHours(hours[i]))
                    .heapSnapshotSummary("Heap at hour " + hours[i] + ": " + heaps[i] + "MB used").build());
        }

        // Guard portal — healthy session for comparison
        browserSnapshots.add(BrowserMemorySnapshot.builder()
                .portalName("Guard Duty Portal").userId("guard-rajan-001").browserVersion("Chrome 120.0.6099.130")
                .sessionId("SESSION-GUARD-001")
                .jsHeapUsedMb(85).jsHeapTotalMb(200).jsHeapLimitMb(4096)
                .domNodeCount(800).detachedDomNodes(5).eventListenerCount(45)
                .websocketConnections(1).websocketMessagesQueued(0).websocketListenersCleaned(true)
                .reactQueryCacheEntries(15).reactQueryStaleCacheEntries(2)
                .reactComponentMountCount(120).reactComponentUnmountMissing(0)
                .primaryLeakSource(LeakSource.NONE)
                .sessionDurationMinutes(480).memoryGrowthRateMbPerHour(2)
                .health(MemoryHealth.HEALTHY)
                .snapshotTimestamp(now.minusMinutes(10))
                .heapSnapshotSummary("Guard portal: stable at 85MB after 8 hours").build());

        browserRepo.saveAll(browserSnapshots);

        // =====================================================
        // RECOMMENDATIONS
        // =====================================================
        List<ProfilingRecommendation> recs = List.of(
                ProfilingRecommendation.builder()
                        .serviceId("SVC-GATE-ROUTING").domain(RuntimeDomain.JVM_GC).severity(Severity.CRITICAL)
                        .title("Migrate Gate Routing to ZGC or Virtual Threads")
                        .description("G1GC Full GC pauses reach 645ms during morning peak. The 4GB heap with 680MB/s allocation rate overwhelms G1's concurrent marking. Options: (1) Switch to ZGC on Java 21 for sub-10ms pauses, (2) Migrate blocking FCM calls to Virtual Threads to reduce heap pressure from parked platform threads, (3) Increase heap to 8GB as interim measure.")
                        .codeSnippet("# JVM flags for ZGC migration:\n-XX:+UseZGC -XX:+ZGenerational -Xmx8g -XX:SoftMaxHeapSize=6g\n\n# Or enable Virtual Threads (Java 21):\nspring.threads.virtual.enabled=true\nserver.tomcat.threads.max=10  # carrier threads only")
                        .status(RecommendationStatus.OPEN).estimatedImpact("GC pause: 645ms → <5ms")
                        .detectedAt(now.minusHours(2)).build(),
                ProfilingRecommendation.builder()
                        .serviceId("SVC-GATE-ROUTING").domain(RuntimeDomain.JVM_THREADS).severity(Severity.CRITICAL)
                        .title("Tomcat Thread Pool Starvation Under FCM Latency Spike")
                        .description("198/200 Tomcat threads blocked waiting on FCM HTTP responses. Queue depth: 847. 234 requests rejected. The blocking I/O model means each pending FCM call holds an OS thread. Virtual Threads would allow 10,000+ concurrent FCM calls on 10 carrier threads.")
                        .codeSnippet("// Before (platform threads, 200 max connections):\n@Bean\npublic RestTemplate fcmClient() {\n    return new RestTemplate(); // blocks platform thread\n}\n\n// After (Virtual Threads + non-blocking):\n@Bean\npublic WebClient fcmClient() {\n    return WebClient.builder()\n        .baseUrl(\"https://fcm.googleapis.com\")\n        .build(); // non-blocking, works with VT\n}")
                        .status(RecommendationStatus.OPEN).estimatedImpact("Throughput: 200 → 10,000+ concurrent")
                        .detectedAt(now.minusHours(1)).build(),
                ProfilingRecommendation.builder()
                        .serviceId("SVC-ERP-NODE").domain(RuntimeDomain.NODEJS_EVENT_LOOP).severity(Severity.CRITICAL)
                        .title("PDF/CSV Exports Block Event Loop for 2.8–4.2 Seconds")
                        .description("Synchronous PDF generation (pdfkit) and CSV serialization (csv-stringify/sync) run on the main V8 thread, blocking all concurrent wallet API requests. During a 12,000-row ledger PDF export, the event loop lag hits 4,200ms — wallet payments stall.")
                        .codeSnippet("// Before (blocks event loop):\napp.get('/export/pdf', (req, res) => {\n  const doc = new PDFDocument();\n  generateLedgerPDF(doc, rows); // 4.2s sync\n  doc.pipe(res);\n});\n\n// After (Worker Thread isolation):\nconst { Worker } = require('worker_threads');\napp.get('/export/pdf', async (req, res) => {\n  const worker = new Worker('./workers/pdf-gen.js', {\n    workerData: { query: req.query }\n  });\n  worker.on('message', (buffer) => res.send(buffer));\n});")
                        .status(RecommendationStatus.IN_PROGRESS).estimatedImpact("Event loop lag: 4200ms → <5ms")
                        .detectedAt(now.minusHours(3)).build(),
                ProfilingRecommendation.builder()
                        .serviceId("SVC-ERP-NODE").domain(RuntimeDomain.NODEJS_MEMORY).severity(Severity.HIGH)
                        .title("Consider BullMQ for Heavy Export Jobs")
                        .description("Worker Threads fix the event loop blocking but still consume memory in-process. For exports >50,000 rows, offload to a Redis-backed BullMQ queue with dedicated worker processes. This isolates memory spikes and allows horizontal scaling of export workers.")
                        .codeSnippet("// BullMQ producer (API server):\nconst exportQueue = new Queue('ledger-exports');\napp.post('/export', async (req, res) => {\n  const job = await exportQueue.add('pdf', req.body);\n  res.json({ jobId: job.id, status: 'queued' });\n});\n\n// BullMQ consumer (separate process):\nconst worker = new BullWorker('ledger-exports', async (job) => {\n  const pdf = await generatePDF(job.data);\n  await uploadToS3(pdf);\n  await notifyUser(job.data.userId, pdf.url);\n});")
                        .status(RecommendationStatus.OPEN).estimatedImpact("Export memory isolated from API process")
                        .detectedAt(now.minusHours(2)).build(),
                ProfilingRecommendation.builder()
                        .serviceId("ADMIN-PORTAL").domain(RuntimeDomain.BROWSER_DOM).severity(Severity.CRITICAL)
                        .title("12,000 Detached DOM Nodes After 10-Hour Admin Session")
                        .description("Admin portal accumulates detached DOM nodes from tab navigation without proper cleanup. React components mount on tab switch but useEffect cleanup functions are missing, leaving event listeners and DOM references in memory. After 10 hours: 42,000 DOM nodes (12,000 detached), 4,500 event listeners, 2.4GB heap.")
                        .codeSnippet("// LEAK: Missing cleanup in visitor log polling\nuseEffect(() => {\n  const interval = setInterval(fetchVisitors, 5000);\n  // ❌ No cleanup! Interval persists after unmount\n}, []);\n\n// FIX: Return cleanup function\nuseEffect(() => {\n  const interval = setInterval(fetchVisitors, 5000);\n  return () => clearInterval(interval); // ✅\n}, []);")
                        .status(RecommendationStatus.OPEN).estimatedImpact("Memory: 2.4GB → stable 200MB")
                        .detectedAt(now.minusHours(1)).build(),
                ProfilingRecommendation.builder()
                        .serviceId("ADMIN-PORTAL").domain(RuntimeDomain.BROWSER_MEMORY).severity(Severity.HIGH)
                        .title("Unbounded React Query Cache Grows to 2,200 Entries")
                        .description("React Query default cache time is Infinity. Every paginated visitor list, maintenance record, and complaint fetch stays cached forever. After 10 hours of tab switching, 2,200 cache entries (1,100 stale) hold 800MB of JSON data in memory.")
                        .codeSnippet("// LEAK: Default cache config\nconst queryClient = new QueryClient();\n\n// FIX: Bound cache lifetime and size\nconst queryClient = new QueryClient({\n  defaultOptions: {\n    queries: {\n      gcTime: 5 * 60 * 1000,     // 5 min GC\n      staleTime: 30 * 1000,       // 30s stale\n      refetchOnWindowFocus: false,\n    },\n  },\n});")
                        .status(RecommendationStatus.OPEN).estimatedImpact("Cache: 2,200 entries → <50 active")
                        .detectedAt(now.minusMinutes(45)).build()
        );
        recsRepo.saveAll(recs);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Runtime profiling demo data seeded");
        result.put("gcEvents", gcEvents.size());
        result.put("threadPoolSnapshots", threadSnapshots.size());
        result.put("eventLoopSnapshots", eventLoopSnapshots.size());
        result.put("browserMemorySnapshots", browserSnapshots.size());
        result.put("recommendations", recs.size());
        return ResponseEntity.ok(result);
    }
}
