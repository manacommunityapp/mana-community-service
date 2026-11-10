package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_okr_key_results")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrOkrKeyResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "okr_id", nullable = false)
    private HrOkr okr;
    @Column(name = "title", nullable = false, length = 400) private String title;
    @Column(name = "target_value", length = 200) private String targetValue;
    @Column(name = "current_value", length = 200) private String currentValue;
    @Column(name = "progress_percent") @Builder.Default private Integer progressPercent = 0;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
