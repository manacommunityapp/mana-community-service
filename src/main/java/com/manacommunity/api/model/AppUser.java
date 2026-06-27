package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BUG FIX: AppUser was an empty stub class.
 * Now fully mapped to the app_user table in the DB schema.
 * - Uses BIGSERIAL PK (Long + IDENTITY strategy)
 * - Includes all required fields: dateOfBirth, gender, role, kyc_status,
 *   govt_id_type, govt_id_number, community FK, is_active, updated_at
 * - @PrePersist / @PreUpdate hooks for audit timestamps
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"roleEntity", "community"})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    // Sensitive: never serialise out (login/registration set it via DTOs, never via AppUser JSON).
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 10)
    private String gender; // MALE, FEMALE, OTHER

    @Column(name = "profile_pic_url", length = 255)
    private String profilePicUrl;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "MEMBER"; // ADMIN, MEMBER, VENDOR

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role roleEntity;

    @Column(name = "kyc_status", nullable = false, length = 20)
    @Builder.Default
    private String kycStatus = "PENDING"; // PENDING, VERIFIED, REJECTED

    @Column(name = "govt_id_type", length = 20)
    private String govtIdType; // AADHAAR, VOTER_ID, DRIVING_LICENCE

    @JsonIgnore
    @Column(name = "govt_id_number", length = 255)
    private String govtIdNumber;

    @Column(name = "flat_no", length = 20)
    private String flatNo;

    @Column(name = "block", length = 20)
    private String block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ── Brute-force lockout (stateless session security) ──────────────────
    // Consecutive failed logins; reset to 0 on a successful login.
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    // When set and in the future, login is refused until this instant.
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
