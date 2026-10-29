package com.manacommunity.api.sports.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sports_external_form")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsExternalForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private SportsEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private FormProvider provider = FormProvider.GOOGLE_FORMS;

    @Column(name = "external_form_id", nullable = false, length = 255)
    private String externalFormId;

    @Column(name = "responder_url", nullable = false, length = 1024)
    private String responderUrl;

    @Column(name = "edit_url", length = 1024)
    private String editUrl;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "webhook_token", unique = true, length = 255)
    private String webhookToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private FormStatus status = FormStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_mode", nullable = false, length = 32)
    @Builder.Default
    private ApprovalMode approvalMode = ApprovalMode.TOURNAMENT_DEFAULT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdBy;

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SportsExternalFormFieldMapping> fieldMappings = new ArrayList<>();

    @OneToOne(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
    private SportsExternalSyncState syncState;

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

    public enum FormProvider { GOOGLE_FORMS, MANA_FORM }
    public enum FormStatus { ACTIVE, PAUSED, CLOSED }
    public enum ApprovalMode { AUTO, MANUAL, TOURNAMENT_DEFAULT }
}
