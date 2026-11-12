package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_passport_certifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MwPassportCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passport_id")
    private MwSkillPassport passport;

    private String name;
    private String issuingBody;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String credentialUrl;
    private Boolean isVerified;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (isVerified == null) isVerified = false;
    }
}
