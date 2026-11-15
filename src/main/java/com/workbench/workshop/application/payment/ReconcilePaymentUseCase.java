package com.workbench.workshop.application.payment;

import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import com.workbench.workshop.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta o gateway e aplica a decisão a um pagamento PENDENTE (job de consulta,
 * {@code payment.provider=mercadopago}). Sem decisão ainda, não faz nada. Guarda o id
 * do pagamento aprovado no provedor, usado depois no estorno.
 */
@Service
@RequiredArgsConstructor
public class ReconcilePaymentUseCase {

    private final PaymentGatewayPort gateway;
    private final PaymentDecisionRecorder decisionRecorder;

    @Transactional
    public Payment execute(Payment payment) {
        var result = gateway.findPayment(payment.chargeReference());
        return switch (result.status()) {
            case APPROVED -> decisionRecorder.record(payment, true, result.transactionId(), "Consulta ao gateway");
            case REJECTED -> decisionRecorder.record(payment, false, null, "Consulta ao gateway");
            case PENDING -> payment;
        };
    }
}
