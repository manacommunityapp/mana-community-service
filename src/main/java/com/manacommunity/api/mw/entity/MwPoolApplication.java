package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mw_pool_applications")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwPoolApplication {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private MwPoolListing listing;

    private Long manaUserId;
    private Long passportId;
    private String status;
    private String workerNote;
    private String employerNote;
    private LocalDateTime appliedAt;
    private LocalDateTime respondedAt;

    @PrePersist
    void onCreate() {
        appliedAt = LocalDateTime.now();
        if (status == null) status = "APPLIED";
    }
}
