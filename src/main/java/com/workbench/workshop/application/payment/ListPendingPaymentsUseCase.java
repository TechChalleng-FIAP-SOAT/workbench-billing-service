package com.workbench.workshop.application.payment;

import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Pagamentos ainda sem decisão do gateway (alvo do job de consulta). */
@Service
@RequiredArgsConstructor
public class ListPendingPaymentsUseCase {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<Payment> execute() {
        return paymentRepository.findByStatus(PaymentStatus.PENDENTE);
    }
}
