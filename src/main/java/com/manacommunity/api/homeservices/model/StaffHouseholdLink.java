package com.manacommunity.api.homeservices.model;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "staff_household_link",
       indexes = {
           @Index(name = "idx_shl_staff", columnList = "staff_id"),
           @Index(name = "idx_shl_resident", columnList = "resident_user_id")
       },
       uniqueConstraints = @UniqueConstraint(name = "uq_shl_staff_resident",
               columnNames = {"staff_id", "resident_user_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StaffHouseholdLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "flat_number", length = 50)
    private String flatNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String schedule;

    @Column(name = "since_date")
    private LocalDate sinceDate;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
