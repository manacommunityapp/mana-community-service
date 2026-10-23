package com.manacommunity.api.serviceplatform.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceUrgency;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "service_request", indexes = {
        @Index(name = "idx_service_request_requester", columnList = "requester_id"),
        @Index(name = "idx_service_request_community", columnList = "community_id"),
        @Index(name = "idx_service_request_category", columnList = "category_id"),
        @Index(name = "idx_service_request_status", columnList = "status"),
        @Index(name = "idx_service_request_provider", columnList = "assigned_provider_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AppUser requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "preferred_time_slot", length = 50)
    private String preferredTimeSlot;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ServiceUrgency urgency = ServiceUrgency.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ServiceRequestStatus status = ServiceRequestStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_provider_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceProvider assignedProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_offering_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProviderServiceOffering assignedOffering;

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 12, scale = 2)
    private BigDecimal actualCost;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_field_values", columnDefinition = "jsonb")
    private String customFieldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String attachments;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

}
