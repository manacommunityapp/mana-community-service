package com.manacommunity.api.analytics.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "community_metrics_snapshots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"community_id", "snapshot_date"}))
public class CommunityMetricsSnapshot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_dues_outstanding")
    private BigDecimal totalDuesOutstanding;

    @Column(name = "collection_rate")
    private BigDecimal collectionRate;

    @Column(name = "avg_payment_delay_days")
    private BigDecimal avgPaymentDelayDays;

    @Column(name = "open_complaints")
    private Integer openComplaints;

    @Column(name = "avg_resolution_hours")
    private BigDecimal avgResolutionHours;

    @Column(name = "overdue_maintenance")
    private Integer overdueMaintenance;

    @Column(name = "active_residents_pct")
    private BigDecimal activeResidentsPct;

    @Column(name = "events_this_month")
    private Integer eventsThisMonth;

    @Column(name = "votes_cast_this_month")
    private Integer votesCastThisMonth;

    @Column(name = "gate_entries_today")
    private Integer gateEntriesToday;

    @Column(name = "unrecognised_entries")
    private Integer unrecognisedEntries;

    @Column(name = "water_kl")
    private BigDecimal waterKl;

    @Column(name = "electricity_kwh")
    private BigDecimal electricityKwh;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public BigDecimal getTotalDuesOutstanding() { return totalDuesOutstanding; }
    public void setTotalDuesOutstanding(BigDecimal v) { this.totalDuesOutstanding = v; }
    public BigDecimal getCollectionRate() { return collectionRate; }
    public void setCollectionRate(BigDecimal v) { this.collectionRate = v; }
    public BigDecimal getAvgPaymentDelayDays() { return avgPaymentDelayDays; }
    public void setAvgPaymentDelayDays(BigDecimal v) { this.avgPaymentDelayDays = v; }
    public Integer getOpenComplaints() { return openComplaints; }
    public void setOpenComplaints(Integer v) { this.openComplaints = v; }
    public BigDecimal getAvgResolutionHours() { return avgResolutionHours; }
    public void setAvgResolutionHours(BigDecimal v) { this.avgResolutionHours = v; }
    public Integer getOverdueMaintenance() { return overdueMaintenance; }
    public void setOverdueMaintenance(Integer v) { this.overdueMaintenance = v; }
    public BigDecimal getActiveResidentsPct() { return activeResidentsPct; }
    public void setActiveResidentsPct(BigDecimal v) { this.activeResidentsPct = v; }
    public Integer getEventsThisMonth() { return eventsThisMonth; }
    public void setEventsThisMonth(Integer v) { this.eventsThisMonth = v; }
    public Integer getVotesCastThisMonth() { return votesCastThisMonth; }
    public void setVotesCastThisMonth(Integer v) { this.votesCastThisMonth = v; }
    public Integer getGateEntriesToday() { return gateEntriesToday; }
    public void setGateEntriesToday(Integer v) { this.gateEntriesToday = v; }
    public Integer getUnrecognisedEntries() { return unrecognisedEntries; }
    public void setUnrecognisedEntries(Integer v) { this.unrecognisedEntries = v; }
    public BigDecimal getWaterKl() { return waterKl; }
    public void setWaterKl(BigDecimal v) { this.waterKl = v; }
    public BigDecimal getElectricityKwh() { return electricityKwh; }
    public void setElectricityKwh(BigDecimal v) { this.electricityKwh = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
