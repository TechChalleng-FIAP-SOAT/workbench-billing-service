package com.workbench.workshop.application.budget.port.out;

import com.workbench.workshop.domain.budget.Budget;

import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {
    Budget save(Budget budget);
    Optional<Budget> findByServiceOrderId(UUID serviceOrderId);

    /** Documento do cliente dono do orçamento da OS (nulo se não há orçamento ou dono). */
    default String ownerOf(UUID serviceOrderId) {
        return findByServiceOrderId(serviceOrderId).map(Budget::getCustomerDocument).orElse(null);
    }
}
