package com.manacommunity.api.cfbos.expense.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_expense_category")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExpenseCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, unique = true, length = 20) private String code;
    private Long parentId;
    private Long accountId;
    @Builder.Default private Boolean isActive = true;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
