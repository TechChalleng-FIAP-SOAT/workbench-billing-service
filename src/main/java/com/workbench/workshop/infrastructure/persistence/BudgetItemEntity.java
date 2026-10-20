package com.workbench.workshop.infrastructure.persistence;

import com.workbench.workshop.domain.budget.ItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "budget_items")
public class BudgetItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "budget_item_seq")
    @SequenceGenerator(name = "budget_item_seq", sequenceName = "budget_item_seq", allocationSize = 50)
    private Long id;
    @Column(nullable = false) private Long budgetId;
    @Column private String productId;               // id do produto no Execution; nulo para MO
    @Column(nullable = false) private String name;
    @Column private String sku;
    @Column private String brand;                   // marca da peça; nulo para MO
    @Enumerated(EnumType.STRING) @Column(name = "item_type", nullable = false) private ItemType itemType;
    @Column(nullable = false) private BigDecimal quantity;
    @Column(nullable = false) private BigDecimal unitPrice;
    @Column(nullable = false) private BigDecimal totalPrice;
}
