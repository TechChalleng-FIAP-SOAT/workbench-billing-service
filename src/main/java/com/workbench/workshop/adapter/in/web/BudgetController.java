package com.workbench.workshop.adapter.in.web;

import com.workbench.workshop.application.budget.FindBudgetUseCase;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.budget.BudgetItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Tag(name = "Budget", description = "Orçamento da OS")
public class BudgetController {

    private final FindBudgetUseCase findBudgetUseCase;

    @GetMapping("/service-order/{serviceOrderId}")
    @Operation(summary = "Buscar orçamento por OS")
    public ResponseEntity<BudgetResponse> findByServiceOrder(@PathVariable UUID serviceOrderId) {
        return findBudgetUseCase.execute(serviceOrderId)
                .map(b -> ResponseEntity.ok(BudgetResponse.from(b)))
                .orElse(ResponseEntity.notFound().build());
    }

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
