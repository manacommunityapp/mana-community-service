package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_prospectus_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventProspectusConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "event_title", length = 200)
    private String eventTitle;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_name", length = 150)
    private String accountName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 50)
    private String ifscCode;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "helplines_json", columnDefinition = "TEXT")
    private String helplinesJson;

    @Column(name = "contacts_json", columnDefinition = "TEXT")
    private String contactsJson;

    @Column(name = "packages_json", columnDefinition = "TEXT")
    private String packagesJson;

    @Column(name = "donations_json", columnDefinition = "TEXT")
    private String donationsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
