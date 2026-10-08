package com.manacommunity.api.access.biometric;

import com.manacommunity.api.access.biometric.BiometricEnums.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_user_enrollments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricUserEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 50)
    private BiometricPersonType personType;

    @Column(name = "person_name", nullable = false, length = 100)
    private String personName;

    @Column(name = "unit_number", length = 50)
    private String unitNumber;

    @Column(name = "face_embedding_hash", nullable = false, length = 255)
    private String faceEmbeddingHash;

    @Builder.Default
    @Column(name = "face_feature_version", nullable = false, length = 50)
    private String faceFeatureVersion = "FaceNet-v2";

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_status", nullable = false, length = 50)
    @Builder.Default
    private EnrollmentStatus enrollmentStatus = EnrollmentStatus.ENROLLED;

    @Builder.Default
    @Column(name = "time_window_start", length = 10)
    private String timeWindowStart = "06:00";

    @Builder.Default
    @Column(name = "time_window_end", length = 10)
    private String timeWindowEnd = "22:00";

    @Builder.Default
    @Column(name = "allowed_days", length = 50)
    private String allowedDays = "MON,TUE,WED,THU,FRI,SAT,SUN";

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Community getCommunity() { return community; }
    public void setCommunity(Community community) { this.community = community; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public BiometricPersonType getPersonType() { return personType; }
    public void setPersonType(BiometricPersonType personType) { this.personType = personType; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }
    public String getFaceEmbeddingHash() { return faceEmbeddingHash; }
    public void setFaceEmbeddingHash(String faceEmbeddingHash) { this.faceEmbeddingHash = faceEmbeddingHash; }
    public String getFaceFeatureVersion() { return faceFeatureVersion; }
    public void setFaceFeatureVersion(String faceFeatureVersion) { this.faceFeatureVersion = faceFeatureVersion; }
    public EnrollmentStatus getEnrollmentStatus() { return enrollmentStatus; }
    public void setEnrollmentStatus(EnrollmentStatus enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }
    public String getTimeWindowStart() { return timeWindowStart; }
    public void setTimeWindowStart(String timeWindowStart) { this.timeWindowStart = timeWindowStart; }
    public String getTimeWindowEnd() { return timeWindowEnd; }
    public void setTimeWindowEnd(String timeWindowEnd) { this.timeWindowEnd = timeWindowEnd; }
    public String getAllowedDays() { return allowedDays; }
    public void setAllowedDays(String allowedDays) { this.allowedDays = allowedDays; }
    public LocalDateTime getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDateTime expirationDate) { this.expirationDate = expirationDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
