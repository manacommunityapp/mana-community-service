package com.manacommunity.api.visitor.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "visitor_group_invite")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvite extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private AppUser host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "pass_code", length = 12, unique = true)
    private String passCode;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "groupInvite", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupGuest> guests = new ArrayList<>();
}
