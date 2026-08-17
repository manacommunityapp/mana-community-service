package com.manacommunity.api.events.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_family_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EventFamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"password", "roles", "community"})
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "createdBy", "community"})
    private CommunityEvent event;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 60)
    private String relation;

    private Integer age;

    @Column(length = 20)
    private String gender;

    @Column(length = 20)
    private String avatar;

    @Column(length = 100)
    private String gothram;

    @Column(length = 100)
    private String nakshatram;

    @Column(length = 100)
    private String rasi;

    @Column(length = 30)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (avatar == null) avatar = "👤";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
