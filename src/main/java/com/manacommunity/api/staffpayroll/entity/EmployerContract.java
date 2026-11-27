package com.manacommunity.api.staffpayroll.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "employer_contracts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployerContract extends BaseAuditEntity {

    public enum SalaryCycleType { FIRST_TO_FIRST, TENTH_TO_TENTH, FIFTEENTH_TO_FIFTEENTH, CUSTOM }
    public enum HalfDayRule { HALF_SALARY, NO_DEDUCTION, FULL_DEDUCTION }
    public enum ContractStatus { ACTIVE, PAUSED, TERMINATED }

    @Column(nullable = false) private Long communityId;
    @Column(nullable = false) private Long staffId;
    @Column(nullable = false) private String staffName;

    @Column(nullable = false) private String employerFlatNumber;
    @Column(nullable = false) private String employerTowerName;
    @Column(nullable = false) private String employerName;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private SalaryCycleType salaryCycle;
    private int cycleStartDay;

    private double monthlySalary;
    private double dailyRate;

    private int paidLeavesPerMonth;
    @Enumerated(EnumType.STRING) private HalfDayRule halfDayRule;
    @Builder.Default private boolean weeklyOffSunday = true;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private ContractStatus status;

    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String terminationReason;
}
