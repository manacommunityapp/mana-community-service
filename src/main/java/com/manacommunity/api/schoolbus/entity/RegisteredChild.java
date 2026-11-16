package com.manacommunity.api.schoolbus.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "school_bus_child")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredChild extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private AppUser parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String grade;

    @Column(name = "school_name", length = 200)
    private String schoolName;

    @Column(name = "bus_id")
    private Long busId;

    @Column(name = "stop_name", length = 100)
    private String stopName;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
