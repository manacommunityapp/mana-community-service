package com.manacommunity.api.visitor.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "visitor_whitelist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhitelistEntry extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private AppUser resident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 15)
    private String phone;

    @Column(length = 30)
    @Builder.Default
    private String category = "GUEST";

    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
