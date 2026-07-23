package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vms_contract_escalation_matrix")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsContractEscalationMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private VmsContract contract;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "contact_name", nullable = false, length = 150)
    private String contactName;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(length = 100)
    private String designation;

    @Column(name = "escalation_time")
    private Integer escalationTime;

    @Column(name = "time_unit", length = 20)
    @Builder.Default
    private String timeUnit = "HOURS";
}
