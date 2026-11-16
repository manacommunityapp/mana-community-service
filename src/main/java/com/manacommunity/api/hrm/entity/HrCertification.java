package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_certifications")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrCertification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @Column(name = "name", nullable = false, length = 300) private String name;
    @Column(name = "issuing_body", length = 200) private String issuingBody;
    @Column(name = "credential_id", length = 200) private String credentialId;
    @Column(name = "issued_date") private LocalDate issuedDate;
    @Column(name = "expiry_date") private LocalDate expiryDate;
    @Column(name = "certificate_url", length = 500) private String certificateUrl;
    @Column(name = "is_verified", nullable = false) @Builder.Default private Boolean isVerified = false;
    @Column(name = "reminder_sent", nullable = false) @Builder.Default private Boolean reminderSent = false;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
