package com.manacommunity.api.visitor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "visitor_group_guest")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupGuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_invite_id", nullable = false)
    private GroupInvite groupInvite;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 15)
    private String phone;

    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "is_checked_in")
    @Builder.Default
    private Boolean isCheckedIn = false;
}
