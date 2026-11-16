package com.workbench.workshop.application.payment;

import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/** Decisão vinda do webhook SIMULADO (só ambiente local, {@code payment.provider=fake}). */
@Service
@RequiredArgsConstructor
public class HandlePaymentNotificationUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentDecisionRecorder decisionRecorder;

    @Transactional
    public Payment execute(String gatewayPaymentId, boolean approved) {
        Payment payment = paymentRepository.findByGatewayPaymentId(gatewayPaymentId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Pagamento não encontrado para o id do gateway: " + gatewayPaymentId));
        return decisionRecorder.record(payment, approved, null, "Webhook simulado");
    }
}
