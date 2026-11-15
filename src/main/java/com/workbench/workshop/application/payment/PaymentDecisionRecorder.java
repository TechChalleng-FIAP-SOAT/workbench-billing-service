package com.workbench.workshop.application.payment;

import com.workbench.workshop.application.payment.port.out.PaymentEventPort;
import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.domain.payment.PaymentHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Aplica a decisão do gateway (aprovado/recusado) a um pagamento: transição, histórico
 * e evento para a Saga. Compartilhado pelo webhook simulado e pelo job de consulta;
 * roda dentro da transação de quem chama.
 */
@Component
@RequiredArgsConstructor
class PaymentDecisionRecorder {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPort paymentEventPort;

    Payment record(Payment payment, boolean approved, String transactionId, String source) {
        // Idempotência: decisão repetida (aprovado sobre aprovado, recusado sobre
        // recusado) não reprocessa nem falha — gateways reenviam a mesma notificação.
        if (approved ? payment.isApproved() : payment.isRejected()) {
            return payment;
        }
        if (approved) {
            payment.approve(transactionId);
        } else {
            payment.reject();
        }
        Payment saved = paymentRepository.save(payment);
        paymentRepository.appendHistory(PaymentHistory.of(saved.getId(), saved.getServiceOrderId(),
                saved.getStatus(), source + ": " + (approved ? "aprovado" : "recusado")));

        // Notifica a Saga DENTRO da transação: se o SNS falhar, a decisão é desfeita e
        // o job de consulta tenta de novo; um evento repetido é descartado pelo
        // orquestrador (só aceita PagamentoConfirmado em AGUARDANDO_PAGAMENTO).
        if (approved) {
            paymentEventPort.paymentConfirmed(saved.getServiceOrderId());
        } else {
            paymentEventPort.paymentFailed(saved.getServiceOrderId(), "pagamento recusado pelo gateway");
        }
        return saved;
    }
}
