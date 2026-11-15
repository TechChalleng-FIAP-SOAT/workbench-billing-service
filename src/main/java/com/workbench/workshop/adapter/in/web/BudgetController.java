package com.workbench.workshop.adapter.in.web;

import com.workbench.workshop.adapter.in.web.BudgetDtos.BudgetResponse;
import com.workbench.workshop.application.budget.FindBudgetUseCase;
import com.workbench.workshop.domain.budget.Budget;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Tag(name = "Budget", description = "Orçamento da OS")
public class BudgetController {

    private final FindBudgetUseCase findBudgetUseCase;

    @GetMapping("/service-order/{serviceOrderId}")
    @Operation(summary = "Buscar orçamento por OS")
    public ResponseEntity<BudgetResponse> findByServiceOrder(@PathVariable UUID serviceOrderId,
                                                             Authentication authentication) {
        return findBudgetUseCase.execute(serviceOrderId, RequesterResolver.from(authentication))
                .map(b -> ResponseEntity.ok(BudgetResponse.from(b)))
                .orElse(ResponseEntity.notFound().build());
    }
}
