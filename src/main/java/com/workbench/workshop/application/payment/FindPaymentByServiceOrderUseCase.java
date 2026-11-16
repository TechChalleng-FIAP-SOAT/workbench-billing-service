package com.workbench.workshop.application.payment;

import com.workbench.workshop.application.Requester;
import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Pagamento da OS; cliente só vê o do próprio orçamento (403 caso contrário). */
@Service
@RequiredArgsConstructor
public class FindPaymentByServiceOrderUseCase {

    private final PaymentRepository paymentRepository;
    private final BudgetRepository budgetRepository;

    @Transactional(readOnly = true)
    public Optional<Payment> execute(UUID serviceOrderId, Requester requester) {
        if (requester.client()) {
            requester.requireAccessTo(budgetRepository.ownerOf(serviceOrderId));
        }
        return paymentRepository.findByServiceOrderId(serviceOrderId);
    }
}
