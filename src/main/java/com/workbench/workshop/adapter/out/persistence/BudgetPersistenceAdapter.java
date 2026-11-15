package com.workbench.workshop.adapter.out.persistence;

import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.budget.BudgetItem;
import com.workbench.workshop.infrastructure.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BudgetPersistenceAdapter implements BudgetRepository {

    private final BudgetJpaRepository budgetJpaRepository;
    private final BudgetItemJpaRepository budgetItemJpaRepository;

    @Override
    @Transactional
    public Budget save(Budget budget) {
        BudgetEntity entity = (budget.getId() != null)
                ? budgetJpaRepository.findById(budget.getId()).orElseGet(BudgetEntity::new)
                : new BudgetEntity();
        entity.setServiceOrderId(budget.getServiceOrderId());
        entity.setCustomerDocument(budget.getCustomerDocument());
        entity.setTotalParts(budget.getTotalParts());
        entity.setTotalLabor(budget.getTotalLabor());
        entity.setTotalPrice(budget.getTotalPrice());
        BudgetEntity saved = budgetJpaRepository.save(entity);

        budgetItemJpaRepository.deleteByBudgetId(saved.getId());
        for (BudgetItem item : budget.getItems()) {
            BudgetItemEntity ie = new BudgetItemEntity();
            ie.setBudgetId(saved.getId());
            ie.setProductId(item.getProductId());
            ie.setName(item.getName());
            ie.setSku(item.getSku());
            ie.setBrand(item.getBrand());
            ie.setItemType(item.getItemType());
            ie.setQuantity(item.getQuantity());
            ie.setUnitPrice(item.getUnitPrice());
            ie.setTotalPrice(item.getTotalPrice());
            budgetItemJpaRepository.save(ie);
        }
        return toDomain(saved);
    }

    @Override
    public Optional<Budget> findByServiceOrderId(UUID serviceOrderId) {
        return budgetJpaRepository.findByServiceOrderId(serviceOrderId).map(this::toDomain);
    }

    private Budget toDomain(BudgetEntity e) {
        List<BudgetItem> items = budgetItemJpaRepository.findByBudgetId(e.getId()).stream()
                .map(this::itemToDomain).toList();
        return Budget.reconstitute(e.getId(), e.getServiceOrderId(), e.getCustomerDocument(),
                e.getTotalParts(), e.getTotalLabor(), e.getTotalPrice(), items,
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private BudgetItem itemToDomain(BudgetItemEntity e) {
        return new BudgetItem(e.getId(), e.getBudgetId(), e.getProductId(), e.getName(), e.getSku(),
                e.getBrand(), e.getItemType(), e.getQuantity(), e.getUnitPrice(), e.getTotalPrice());
    }
}
