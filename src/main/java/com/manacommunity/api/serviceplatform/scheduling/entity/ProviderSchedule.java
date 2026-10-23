package com.manacommunity.api.serviceplatform.scheduling.entity;

import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "sp_provider_schedule")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ServiceProvider provider;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 1 = Monday, 7 = Sunday

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_duration_minutes", nullable = false)
    @Builder.Default
    private Integer slotDurationMinutes = 60;

    @Column(name = "buffer_time_minutes", nullable = false)
    @Builder.Default
    private Integer bufferTimeMinutes = 15;

    @Column(name = "max_parallel_jobs", nullable = false)
    @Builder.Default
    private Integer maxParallelJobs = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
