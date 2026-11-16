package com.workbench.workshop.application.budget;

import com.workbench.workshop.application.Requester;
import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.domain.budget.Budget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FindBudgetUseCase {

    private final BudgetRepository budgetRepository;

    /** Orçamento da OS; cliente só vê o próprio (403 caso contrário). */
    @Transactional(readOnly = true)
    public Optional<Budget> execute(UUID serviceOrderId, Requester requester) {
        Optional<Budget> budget = budgetRepository.findByServiceOrderId(serviceOrderId);
        budget.ifPresent(b -> requester.requireAccessTo(b.getCustomerDocument()));
        return budget;
    }
}
