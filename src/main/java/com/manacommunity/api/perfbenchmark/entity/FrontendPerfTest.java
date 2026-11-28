package com.manacommunity.api.perfbenchmark.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "frontend_perf_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FrontendPerfTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private FrontendScenario scenario;

    private String pageName;
    private double lcpMs;
    private double targetLcpMs;
    private double fidMs;
    private double targetFidMs;
    private double clsScore;
    private double targetCls;
    private double ttfbMs;
    private int bundleSizeKb;
    private int targetBundleSizeKb;
    private int lighthouseScore;
    private boolean lazyLoadEnabled;
    private boolean imageSrcsetUsed;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum FrontendScenario {
        DASHBOARD_LCP_BUDGET, VISITOR_LOG_FID,
        PAYMENT_PAGE_CLS_SHIFT, GATE_PASS_TTFB,
        BUNDLE_SIZE_REGRESSION, IMAGE_LAZY_LOAD,
        SERVICE_WORKER_CACHE, WEBP_CONVERSION
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
