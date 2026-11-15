package com.workbench.workshop.application.payment;

import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.domain.payment.PaymentHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Estorna um pagamento aprovado (compensação da Saga): devolve o dinheiro no gateway
 * pelo id da transação aprovada e marca ESTORNADO. Se o gateway falhar, a transação
 * é desfeita e o pagamento continua aprovado (o comando volta para a fila).
 */
@Service
@RequiredArgsConstructor
public class RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort gateway;

    @Transactional
    public Payment execute(UUID serviceOrderId) {
        Payment payment = paymentRepository.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new NoSuchElementException("Pagamento não encontrado para a OS: " + serviceOrderId));
        payment.refund();
        // Aprovado pelo webhook simulado (fake) não tem transação no provedor: nada a devolver lá.
        if (payment.getGatewayTransactionId() != null) {
            gateway.refund(payment.getGatewayTransactionId());
        }
        Payment saved = paymentRepository.save(payment);
        paymentRepository.appendHistory(PaymentHistory.of(saved.getId(), serviceOrderId,
                saved.getStatus(), "Estorno (compensação da Saga)"));
        return saved;
    }
}
