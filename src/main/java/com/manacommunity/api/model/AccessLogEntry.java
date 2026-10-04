package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "turnstile_code", nullable = false, length = 50)
    private String turnstileCode;

    @Column(name = "turnstile_name", length = 150)
    private String turnstileName;

    @Column(nullable = false, length = 30)
    private String decision;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_full_name", length = 150)
    private String userFullName;

    @Column(name = "user_type", length = 20)
    private String userType;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
