package com.workbench.workshop.adapter.out.persistence;

import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.budget.BudgetItem;
import com.workbench.workshop.infrastructure.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
        Budget b = new Budget();
        b.setId(e.getId());
        b.setServiceOrderId(e.getServiceOrderId());
        b.setTotalParts(e.getTotalParts());
        b.setTotalLabor(e.getTotalLabor());
        b.setTotalPrice(e.getTotalPrice());
        b.setCreatedAt(e.getCreatedAt());
        b.setUpdatedAt(e.getUpdatedAt());
        List<BudgetItem> items = budgetItemJpaRepository.findByBudgetId(e.getId()).stream()
                .map(this::itemToDomain).toList();
        b.setItems(new ArrayList<>(items));
        return b;
    }

    private BudgetItem itemToDomain(BudgetItemEntity e) {
        BudgetItem i = new BudgetItem();
        i.setId(e.getId());
        i.setBudgetId(e.getBudgetId());
        i.setProductId(e.getProductId());
        i.setName(e.getName());
        i.setSku(e.getSku());
        i.setBrand(e.getBrand());
        i.setItemType(e.getItemType());
        i.setQuantity(e.getQuantity());
        i.setUnitPrice(e.getUnitPrice());
        i.setTotalPrice(e.getTotalPrice());
        return i;
    }
}
