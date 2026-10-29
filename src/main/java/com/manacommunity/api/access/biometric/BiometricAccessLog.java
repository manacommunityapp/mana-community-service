package com.manacommunity.api.access.biometric;

import com.manacommunity.api.access.biometric.BiometricEnums.*;
import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_access_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turnstile_id", nullable = false)
    private BiometricTurnstile turnstile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 50)
    private BiometricPersonType personType;

    @Column(name = "person_name", nullable = false, length = 100)
    private String personName;

    @Column(name = "unit_number", length = 50)
    private String unitNumber;

    @Column(name = "confidence_score", nullable = false, precision = 6, scale = 4)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_decision", nullable = false, length = 50)
    private AccessDecision accessDecision;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "snapshot_url", length = 255)
    private String snapshotUrl;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BiometricTurnstile getTurnstile() { return turnstile; }
    public void setTurnstile(BiometricTurnstile turnstile) { this.turnstile = turnstile; }
    public Community getCommunity() { return community; }
    public void setCommunity(Community community) { this.community = community; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BiometricPersonType getPersonType() { return personType; }
    public void setPersonType(BiometricPersonType personType) { this.personType = personType; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public AccessDecision getAccessDecision() { return accessDecision; }
    public void setAccessDecision(AccessDecision accessDecision) { this.accessDecision = accessDecision; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getSnapshotUrl() { return snapshotUrl; }
    public void setSnapshotUrl(String snapshotUrl) { this.snapshotUrl = snapshotUrl; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
