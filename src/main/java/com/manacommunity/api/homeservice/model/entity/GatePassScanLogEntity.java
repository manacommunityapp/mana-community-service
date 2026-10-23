package com.manacommunity.api.homeservice.model.entity;

import com.manacommunity.api.homeservice.model.enums.HomeServiceScanType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gate_pass_scan_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatePassScanLogEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "gate_pass_id", nullable = false, length = 64)
    private String gatePassId;

    @Column(name = "worker_id", nullable = false, length = 64)
    private String workerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type", nullable = false, length = 20)
    private HomeServiceScanType scanType;

    @Column(name = "gate_name", length = 50)
    private String gateName;

    @Column(name = "guard_user_id", length = 64)
    private String guardUserId;

    @Column(name = "guard_name", length = 150)
    private String guardName;

    @Column(name = "scan_time", nullable = false)
    @Builder.Default
    private LocalDateTime scanTime = LocalDateTime.now();
}
