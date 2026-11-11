package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mw_skill_passports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MwSkillPassport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long manaUserId;
    private Boolean isPublic;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String location;
    private String preferredRoles;
    private String availability;

    @OneToMany(mappedBy = "passport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MwPassportSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "passport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MwPassportCertification> certifications = new ArrayList<>();

    @OneToMany(mappedBy = "passport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MwPassportWorkHistory> workHistory = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isPublic == null) isPublic = false;
        if (availability == null) availability = "UNAVAILABLE";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
