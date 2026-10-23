package com.manacommunity.api.serviceplatform.scheduling.entity;

import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "sp_provider_time_off")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderTimeOff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ServiceProvider provider;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private Boolean isApproved = true;
}
