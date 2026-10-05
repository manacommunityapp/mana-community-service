package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turnstile_id", nullable = false)
    private Turnstile turnstile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccessDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessMethod method;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    @Builder.Default
    private Boolean granted = true;

    @Column(name = "denial_reason", length = 200)
    private String denialReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AccessDirection {
        ENTRY, EXIT
    }

    public enum AccessMethod {
        FINGERPRINT, FACE, CARD, QR
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (granted == null) granted = true;
    }
}
