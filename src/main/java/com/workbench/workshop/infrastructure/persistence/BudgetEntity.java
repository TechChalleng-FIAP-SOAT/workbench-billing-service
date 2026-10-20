package com.workbench.workshop.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "budgets")
public class BudgetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "budget_seq")
    @SequenceGenerator(name = "budget_seq", sequenceName = "budget_seq", allocationSize = 50)
    private Long id;
    @Column(nullable = false, unique = true) private UUID serviceOrderId;
    @Column(name = "total_parts", nullable = false) private BigDecimal totalParts = BigDecimal.ZERO;
    @Column(name = "total_labor", nullable = false) private BigDecimal totalLabor = BigDecimal.ZERO;
    @Column(nullable = false) private BigDecimal totalPrice = BigDecimal.ZERO;
    @Column(updatable = false) private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); this.updatedAt = this.createdAt; }
    @PreUpdate protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
