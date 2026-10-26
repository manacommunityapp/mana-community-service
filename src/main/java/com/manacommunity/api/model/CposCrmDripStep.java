package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cpos_crm_drip_step")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CposCrmDripStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sequence_id", nullable = false)
    private CposCrmDripSequence sequence;

    @Column(name = "delay_days", nullable = false)
    private int delayDays;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;
}
