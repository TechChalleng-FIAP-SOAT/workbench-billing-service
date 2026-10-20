package com.workbench.workshop.application.budget;

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

    @Transactional(readOnly = true)
    public Optional<Budget> execute(UUID serviceOrderId) {
        return budgetRepository.findByServiceOrderId(serviceOrderId);
    }
}
