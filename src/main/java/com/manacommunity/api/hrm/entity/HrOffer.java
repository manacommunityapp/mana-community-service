package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_offers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrOffer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "applicant_id", nullable = false)
    private HrApplicant applicant;
    @Column(name = "offered_salary", nullable = false) private Double offeredSalary;
    @Column(name = "proposed_joining_date") private LocalDate proposedJoiningDate;
    @Column(name = "offer_letter_url", length = 500) private String offerLetterUrl;
    @Column(name = "expiry_date") private LocalDate expiryDate;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "PENDING";
    @Column(name = "response_date") private LocalDate responseDate;
    @Column(name = "notes", length = 500) private String notes;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
