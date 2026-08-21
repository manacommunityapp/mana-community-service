package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_who_to_call_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityWhoToCallHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "who_to_call_id", nullable = false)
    private Long whoToCallId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 30)
    private String action; // CREATED, UPDATED, DEACTIVATED, RESTORED, DELETED

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "changed_by_name", length = 100)
    private String changedByName;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @Column(name = "phone_number", nullable = false, length = 25)
    private String phoneNumber;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "snapshot_data", columnDefinition = "TEXT")
    private String snapshotData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
