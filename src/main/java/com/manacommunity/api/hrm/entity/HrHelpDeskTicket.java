package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_helpdesk_tickets")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrHelpDeskTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @Column(name = "ticket_number", nullable = false, length = 20) private String ticketNumber;
    @Column(name = "category", nullable = false, length = 30) private String category;
    @Column(name = "subject", nullable = false, length = 300) private String subject;
    @Column(name = "description", nullable = false, columnDefinition = "TEXT") private String description;
    @Column(name = "priority", nullable = false, length = 10) @Builder.Default private String priority = "MEDIUM";
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "OPEN";
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to_employee_id")
    private HrEmployee assignedTo;
    @Column(name = "resolution", columnDefinition = "TEXT") private String resolution;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
