package com.manacommunity.api.serviceplatform.interiors.entity;

import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name="interior_change_request", indexes=@Index(name="idx_interior_change_project",columnList="service_request_id,created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InteriorChangeRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_request_id",nullable=false) private ServiceRequest serviceRequest;
    @Column(name="proposed_by",nullable=false) private Long proposedBy;
    @Column(nullable=false,columnDefinition="TEXT") private String description;
    @Column(name="scope_delta",columnDefinition="TEXT") private String scopeDelta;
    @Column(name="price_delta",precision=12,scale=2) private BigDecimal priceDelta;
    @Column(name="proposed_start_date") private LocalDate proposedStartDate;
    @Column(name="proposed_end_date") private LocalDate proposedEndDate;
    @Column(nullable=false,length=20) @Builder.Default private String status="PENDING";
    @Column(name="decision_note",columnDefinition="TEXT") private String decisionNote;
    @Column(name="decided_by") private Long decidedBy;
    @Column(name="decided_at") private LocalDateTime decidedAt;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @PrePersist void onCreate(){createdAt=LocalDateTime.now();}
}
