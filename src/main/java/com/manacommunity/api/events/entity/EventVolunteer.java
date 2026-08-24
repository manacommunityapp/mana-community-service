package com.manacommunity.api.events.entity;


import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_volunteer", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventVolunteer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommunityEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @jakarta.persistence.Transient
    private CommunityEvent communityEvent;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    @Column(name = "user_name", length = 200)
    private String userName;


    @Column(length = 100)
    private String role;

    @Column(length = 100)
    private String zone;


    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private VolunteerStatus volunteerStatus = VolunteerStatus.ASSIGNED;

    @Column(length = 100)
    private String shift;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";


    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum VolunteerStatus {
        ASSIGNED, CHECKED_IN, CHECKED_OUT, NO_SHOW
    }

}
