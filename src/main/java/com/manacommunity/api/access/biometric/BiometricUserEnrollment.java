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
}
