package com.cpn.domain.company.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends TenantAwareEntity {

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, unique = true)
    private String companySlug;

    private String logoUrl;
    private String coverUrl;
    private String industry;
    private String companySize;
    private String website;

    @Column(length = 2000)
    private String description;

    private String headquartersLocation;
    private boolean isVerified;
    private boolean isHiring;
}
