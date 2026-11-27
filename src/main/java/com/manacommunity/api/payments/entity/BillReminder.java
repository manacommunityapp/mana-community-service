package com.manacommunity.api.payments.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bill_reminder")
@Data
public class BillReminder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "bill_type", nullable = false, length = 100) private String billType;
    @Column(name = "biller_name", nullable = false, length = 200) private String billerName;
    @Column(precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "reminder_days") private Integer reminderDays = 3;
    @Column(name = "reminder_sent") private Boolean reminderSent = false;
    @Column(name = "reminder_sent_at") private Instant reminderSentAt;
    @Column private Boolean paid = false;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
