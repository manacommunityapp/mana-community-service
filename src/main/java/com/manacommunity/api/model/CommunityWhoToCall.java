package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_who_to_call")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityWhoToCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "phone_number", nullable = false, length = 25)
    private String phoneNumber;

    @Column(name = "secondary_phone", length = 25)
    private String secondaryPhone;

    @Column(length = 120)
    private String email;

    @Column(length = 100)
    private String designation;

    @Column(length = 100)
    private String availability;

    @Column(name = "location_or_desk", length = 100)
    private String locationOrDesk;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String icon = "HelpCircle";

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String color = "text-indigo-600 bg-indigo-50 border-indigo-200";

    @Column(name = "is_emergency", nullable = false)
    @Builder.Default
    private Boolean isEmergency = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
