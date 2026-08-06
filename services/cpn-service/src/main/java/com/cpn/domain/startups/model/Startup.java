package com.cpn.domain.startups.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "startups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Startup extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID founderId;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String pitch;

    private String stage; // IDEA, PRE_SEED, SEED, SERIES_A
    private String sector;
    private String mrr;
    private String lookingFor; // CO_FOUNDER, INVESTMENT, TALENT
}
