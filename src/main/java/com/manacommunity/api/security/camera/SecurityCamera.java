package com.manacommunity.api.security.camera;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_camera")
@Data
public class SecurityCamera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long communityId;

    @Column(length = 100)
    private String name;

    @Column(length = 200)
    private String location;

    @Column(length = 50)
    private String type; // DOME / BULLET / PTZ / FISHEYE

    @Column(length = 20)
    private String status = "ONLINE"; // ONLINE / OFFLINE / MAINTENANCE

    @Column(length = 500)
    private String streamUrl;

    @Column(length = 500)
    private String snapshotUrl;

    private LocalDateTime lastSnapshotAt;

    private LocalDate installDate;

    @Column(length = 50)
    private String ipAddress;

    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
