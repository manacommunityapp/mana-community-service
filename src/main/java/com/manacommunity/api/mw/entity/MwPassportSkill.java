package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_passport_skills")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MwPassportSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passport_id")
    private MwSkillPassport passport;

    private String skillName;
    private String category;
    private String proficiency;
    private BigDecimal yearsExp;
    private Boolean isVerified;
    private String verifiedBy;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (isVerified == null) isVerified = false;
        if (proficiency == null) proficiency = "INTERMEDIATE";
    }
}
