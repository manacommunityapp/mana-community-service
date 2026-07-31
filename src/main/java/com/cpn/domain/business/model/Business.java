package com.cpn.domain.business.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String businessName;

    private String category;

    @Column(length = 2000)
    private String description;

    private String contactPhone;
    private String flatNumber;
    private boolean isVerified;
}
