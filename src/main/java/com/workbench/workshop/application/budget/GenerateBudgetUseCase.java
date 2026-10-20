package com.workbench.workshop.application.budget;

import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.budget.BudgetItem;
import com.workbench.workshop.domain.budget.ItemType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gera o orçamento da OS a partir dos itens recebidos (snapshot), combinando:
 *   a) peças/insumos (do Execution, já precificados);
 *   b) mão de obra (do OS Service — custo por serviço).
 * O total = Σ(a) + Σ(b), como no orçamento real (ADR-0005).
 *
 * <p>O Billing não acessa estoque nem catálogo de serviços: tudo chega no comando.
 */
@Service
@RequiredArgsConstructor
public class GenerateBudgetUseCase {

    private final BudgetRepository budgetRepository;

    @Transactional
    public Budget execute(UUID serviceOrderId, List<PartInput> parts, List<LaborInput> labor) {
        Budget budget = budgetRepository.findByServiceOrderId(serviceOrderId).orElseGet(() -> {
            Budget b = new Budget();
            b.setServiceOrderId(serviceOrderId);
            return b;
        });

        List<BudgetItem> items = new ArrayList<>();
        for (PartInput p : parts) {
            items.add(BudgetItem.partSnapshot(p.productId(), p.name(), p.sku(), p.brand(), p.itemType(),
                    p.unitPrice(), p.quantity()));
        }
        for (LaborInput l : labor) {
            items.add(BudgetItem.laborSnapshot(l.serviceName(), l.laborCode(), l.laborCost()));
        }

        budget.setItems(items);
        budget.recalculateTotals();
        return budgetRepository.save(budget);
    }

    /** Item de peça/insumo (itemType = PART ou SUPPLY). */
    public record PartInput(String productId, String name, String sku, String brand, ItemType itemType,
                            BigDecimal unitPrice, BigDecimal quantity) {}

    /** Item de mão de obra (custo fixo do serviço). */
    public record LaborInput(String serviceName, String laborCode, BigDecimal laborCost) {}
}
