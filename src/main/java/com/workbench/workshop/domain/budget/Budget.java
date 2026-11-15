package com.workbench.workshop.domain.budget;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Orçamento da OS: peças/insumos (parte a) + mão de obra (parte b). Os totais são
 * sempre derivados dos itens — por isso não há setters: a única forma de mudar o
 * orçamento é {@link #revise}, que recalcula tudo.
 */
@Getter
public class Budget {

    private Long id;
    private UUID serviceOrderId;
    private String customerDocument;                   // CPF/CNPJ do dono (só dígitos)
    private BigDecimal totalParts = BigDecimal.ZERO;   // Σ peças/insumos (parte a)
    private BigDecimal totalLabor = BigDecimal.ZERO;   // Σ mão de obra (parte b)
    private BigDecimal totalPrice = BigDecimal.ZERO;   // a + b
    private List<BudgetItem> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Budget() {}

    /** Orçamento novo (ainda sem itens) para a OS. */
    public static Budget forServiceOrder(UUID serviceOrderId) {
        Budget b = new Budget();
        b.serviceOrderId = Objects.requireNonNull(serviceOrderId, "Ordem de serviço é obrigatória");
        return b;
    }

    /** Reconstrói a partir da persistência (os totais vêm gravados). */
    public static Budget reconstitute(Long id, UUID serviceOrderId, String customerDocument,
                                      BigDecimal totalParts, BigDecimal totalLabor, BigDecimal totalPrice,
                                      List<BudgetItem> items, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Budget b = new Budget();
        b.id = id;
        b.serviceOrderId = serviceOrderId;
        b.customerDocument = customerDocument;
        b.totalParts = totalParts;
        b.totalLabor = totalLabor;
        b.totalPrice = totalPrice;
        b.items = new ArrayList<>(items == null ? List.of() : items);
        b.createdAt = createdAt;
        b.updatedAt = updatedAt;
        return b;
    }

    /** Substitui os itens (snapshot vindo da Saga), registra o dono e recalcula os totais. */
    public void revise(String customerDocument, List<BudgetItem> newItems) {
        this.customerDocument = customerDocument;
        this.items = new ArrayList<>(newItems);
        recalculateTotals();
    }

    // Totais = peças + mão de obra (ADR-0005 / PDF real).
    private void recalculateTotals() {
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
