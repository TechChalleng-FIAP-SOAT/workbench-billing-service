package com.workbench.workshop.application.payment;

import com.workbench.workshop.application.Requester;
import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.payment.PaymentHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Histórico de pagamento da OS, com a mesma regra de dono do pagamento. */
@Service
@RequiredArgsConstructor
public class FindPaymentHistoryUseCase {

    private final PaymentRepository paymentRepository;
    private final BudgetRepository budgetRepository;

    @Transactional(readOnly = true)
    public List<PaymentHistory> execute(UUID serviceOrderId, Requester requester) {
        if (requester.client()) {
            requester.requireAccessTo(budgetRepository.ownerOf(serviceOrderId));
        }
        return paymentRepository.findHistory(serviceOrderId);
    }
}
