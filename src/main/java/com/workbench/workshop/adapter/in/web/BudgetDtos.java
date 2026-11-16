package com.workbench.workshop.adapter.in.web;

import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.budget.BudgetItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** DTOs de saída do orçamento. */
public final class BudgetDtos {

    private BudgetDtos() {}

    public record BudgetResponse(Long id, UUID serviceOrderId,
                                 BigDecimal totalParts, BigDecimal totalLabor, BigDecimal totalPrice,
                                 List<ItemResponse> items, LocalDateTime createdAt, LocalDateTime updatedAt) {
        static BudgetResponse from(Budget b) {
            return new BudgetResponse(b.getId(), b.getServiceOrderId(),
                    b.getTotalParts(), b.getTotalLabor(), b.getTotalPrice(),
                    b.getItems().stream().map(ItemResponse::from).toList(), b.getCreatedAt(), b.getUpdatedAt());
        }
    }

    public record ItemResponse(String productId, String name, String sku, String brand, String itemType,
                               BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
        static ItemResponse from(BudgetItem i) {
            return new ItemResponse(i.getProductId(), i.getName(), i.getSku(), i.getBrand(),
                    i.getItemType().name(), i.getQuantity(), i.getUnitPrice(), i.getTotalPrice());
        }
    }
}
