package com.manacommunity.api.hrm.entity;

import com.manacommunity.api.hrm.enums.HrmRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hr_employee_roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrEmployeeRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HrmRole role;

    @Column(name = "granted_by")
    private Long grantedBy;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @PrePersist
    void onCreate() {
        grantedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
    }
}
