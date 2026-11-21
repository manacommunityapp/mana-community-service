package com.manacommunity.api.analytics.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "community_insights")
public class CommunityInsight {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "insight_type", nullable = false, length = 50)
    private String insightType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity = "INFO";

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "metric_value")
    private BigDecimal metricValue;

    @Column(name = "metric_unit", length = 50)
    private String metricUnit;

    @Column(name = "reference_period", length = 50)
    private String referencePeriod;

    @Column(name = "is_dismissed", nullable = false)
    private boolean dismissed = false;

    @Column(name = "dismissed_by")
    private Long dismissedBy;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt = OffsetDateTime.now();

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    public Long getId() { return id; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long v) { this.communityId = v; }
    public String getInsightType() { return insightType; }
    public void setInsightType(String v) { this.insightType = v; }
    public String getSeverity() { return severity; }
    public void setSeverity(String v) { this.severity = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String v) { this.recommendation = v; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal v) { this.metricValue = v; }
    public String getMetricUnit() { return metricUnit; }
    public void setMetricUnit(String v) { this.metricUnit = v; }
    public String getReferencePeriod() { return referencePeriod; }
    public void setReferencePeriod(String v) { this.referencePeriod = v; }
    public boolean isDismissed() { return dismissed; }
    public void setDismissed(boolean v) { this.dismissed = v; }
    public Long getDismissedBy() { return dismissedBy; }
    public void setDismissedBy(Long v) { this.dismissedBy = v; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime v) { this.expiresAt = v; }
}
