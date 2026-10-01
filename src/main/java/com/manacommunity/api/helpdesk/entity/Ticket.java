package com.manacommunity.api.helpdesk.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "helpdesk_ticket")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 15)
    private String ticketNumber;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 3000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TicketCategory category = TicketCategory.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "admin_remarks", length = 2000)
    private String adminRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by", nullable = false)
    private AppUser raisedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private AppUser assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "sla_due_at")
    private LocalDateTime slaDueAt;

    @Column(name = "is_escalated", nullable = false)
    @Builder.Default
    private boolean escalated = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalation_level", nullable = false)
    @Builder.Default
    private int escalationLevel = 0;

    @Column(name = "satisfaction_rating")
    private Integer satisfactionRating; // 1 to 5

    @Column(name = "feedback_remarks", length = 1000)
    private String feedbackRemarks;

    @Column(name = "resident_signoff", nullable = false)
    @Builder.Default
    private boolean residentSignoff = false;

    @Column(name = "resident_signoff_at")
    private LocalDateTime residentSignoffAt;

    @Column(name = "attachments", length = 2000)
    private String attachments; // Comma-separated or JSON array of URLs

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<TicketComment> comments = new ArrayList<>();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public enum TicketCategory { GENERAL, PLUMBING, ELECTRICAL, SECURITY, PARKING, NOISE, CLEANLINESS, ELEVATOR, OTHER }
    public enum TicketPriority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum TicketStatus { OPEN, IN_PROGRESS, RESOLVED, CLOSED, REJECTED }
}
