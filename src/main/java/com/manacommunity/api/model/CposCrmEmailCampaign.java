package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cpos_crm_email_campaign")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CposCrmEmailCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(length = 255, nullable = false)
    private String subject;

    @Column(name = "body_html", columnDefinition = "TEXT", nullable = false)
    private String bodyHtml;

    @Column(name = "filter_stage", length = 50)
    private String filterStage;

    @Column(name = "filter_source", length = 50)
    private String filterSource;

    @Column(name = "filter_tag_id")
    private Long filterTagId;

    @Column(name = "sent_count", nullable = false)
    @Builder.Default
    private int sentCount = 0;

    @Column(name = "created_by", length = 255, nullable = false)
    private String createdBy;

    @Column(name = "sent_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();
}
