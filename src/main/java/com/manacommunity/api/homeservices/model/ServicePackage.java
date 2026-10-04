package com.manacommunity.api.homeservices.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_package", indexes = {
        @Index(name = "idx_service_package_staff", columnList = "staff_id"),
        @Index(name = "idx_service_package_resident", columnList = "resident_id"),
        @Index(name = "idx_service_package_payment_status", columnList = "payment_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private DomesticStaff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private AppUser resident;

    @Column(name = "flat_number", length = 20)
    private String flatNumber;

    @ElementCollection
    @CollectionTable(name = "service_package_services", joinColumns = @JoinColumn(name = "package_id"))
    @Column(name = "service_name")
    @Builder.Default
    private List<String> services = new ArrayList<>();

    @Column(name = "monthly_salary")
    private BigDecimal monthlySalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.DUE;

    @Column(name = "last_paid_date")
    private LocalDate lastPaidDate;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum PaymentStatus {
        PAID, DUE, OVERDUE
    }
}
