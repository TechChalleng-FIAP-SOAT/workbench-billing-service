package com.workbench.workshop.application.budget;

import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.budget.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GenerateBudgetUseCaseTest {

    private BudgetRepository budgetRepository;
    private GenerateBudgetUseCase useCase;
    private final UUID osId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        budgetRepository = mock(BudgetRepository.class);
        useCase = new GenerateBudgetUseCase(budgetRepository);
        when(budgetRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void generatesBudgetWithPartsAndLaborTotals() {
        when(budgetRepository.findByServiceOrderId(osId)).thenReturn(Optional.empty());
        var parts = List.of(
                new GenerateBudgetUseCase.PartInput("1", "Pastilha", "PF-1", "Marca X", ItemType.PART,
                        new BigDecimal("50.00"), new BigDecimal("2")),   // 100
                new GenerateBudgetUseCase.PartInput("2", "Óleo", "OL-1", "Marca Y", ItemType.SUPPLY,
                        new BigDecimal("30.00"), new BigDecimal("1")));  // 30
        var labor = List.of(
                new GenerateBudgetUseCase.LaborInput("DISCO E PASTILHA", "00", new BigDecimal("200.00"))); // 200

        Budget budget = useCase.execute(osId, parts, labor);

        assertEquals(0, new BigDecimal("130.00").compareTo(budget.getTotalParts()));
        assertEquals(0, new BigDecimal("200.00").compareTo(budget.getTotalLabor()));
        assertEquals(0, new BigDecimal("330.00").compareTo(budget.getTotalPrice()));
        assertEquals(3, budget.getItems().size());
        assertEquals("Marca X", budget.getItems().get(0).getBrand());
        assertNull(budget.getItems().get(2).getBrand()); // MO não tem marca
    }

    @Test
    void generatesBudgetWithoutLabor() {
        when(budgetRepository.findByServiceOrderId(osId)).thenReturn(Optional.empty());
        var parts = List.of(new GenerateBudgetUseCase.PartInput("1", "X", "S", null, ItemType.PART,
                new BigDecimal("10.00"), new BigDecimal("1")));

        Budget budget = useCase.execute(osId, parts, List.of());

        assertEquals(0, new BigDecimal("10.00").compareTo(budget.getTotalParts()));
        assertEquals(0, BigDecimal.ZERO.compareTo(budget.getTotalLabor()));
        assertEquals(0, new BigDecimal("10.00").compareTo(budget.getTotalPrice()));
    }

    @Test
    void regeneratesExistingBudget() {
        Budget existing = new Budget();
        existing.setId(5L);
        existing.setServiceOrderId(osId);
        when(budgetRepository.findByServiceOrderId(osId)).thenReturn(Optional.of(existing));

        var labor = List.of(new GenerateBudgetUseCase.LaborInput("MO", "01", new BigDecimal("80.00")));
        Budget budget = useCase.execute(osId, List.of(), labor);

        assertEquals(5L, budget.getId());
        assertEquals(0, new BigDecimal("80.00").compareTo(budget.getTotalLabor()));
    }
}
