package com.workbench.workshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetItemJpaRepository extends JpaRepository<BudgetItemEntity, Long> {
    List<BudgetItemEntity> findByBudgetId(Long budgetId);
    void deleteByBudgetId(Long budgetId);
}
