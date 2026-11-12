package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_passport_work_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MwPassportWorkHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passport_id")
    private MwSkillPassport passport;

    private String employerName;
    private String role;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
    private Boolean isManaVerified;
    private Long hrEmployeeId;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (isCurrent == null) isCurrent = false;
        if (isManaVerified == null) isManaVerified = false;
    }
}
