package com.workbench.workshop.domain.budget;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
    private Long id;
    private UUID serviceOrderId;
    private BigDecimal totalParts = BigDecimal.ZERO;   // Σ peças/insumos (parte a)
    private BigDecimal totalLabor = BigDecimal.ZERO;   // Σ mão de obra (parte b)
    private BigDecimal totalPrice = BigDecimal.ZERO;   // a + b
    private List<BudgetItem> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Recalcula os totais a partir dos itens: peças + mão de obra (ADR-0005 / PDF real). */
    public void recalculateTotals() {
        this.totalParts = items.stream()
                .filter(i -> !i.isLabor())
                .map(BudgetItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalLabor = items.stream()
                .filter(BudgetItem::isLabor)
                .map(BudgetItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalPrice = this.totalParts.add(this.totalLabor);
    }
}
