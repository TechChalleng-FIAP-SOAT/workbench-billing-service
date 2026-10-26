package com.workbench.workshop.infrastructure.messaging;

import com.workbench.workshop.application.payment.port.out.PaymentEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Publica no barramento da Saga o resultado do pagamento vindo do webhook do gateway.
 * Ativo quando {@code messaging.enabled=true}. O sagaId é o próprio id da OS.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SnsPaymentEventAdapter implements PaymentEventPort {

    private final BillingEventPublisher publisher;

    @Override
    public void paymentConfirmed(UUID serviceOrderId) {
        String os = serviceOrderId.toString();
        log.info("Publicando PagamentoConfirmado: serviceOrderId={}", os);
        publisher.publish(MessageEnvelope.of("PagamentoConfirmado", os, os, Map.of()));
    }

    @Override
    public void paymentFailed(UUID serviceOrderId, String reason) {
        String os = serviceOrderId.toString();
        log.info("Publicando PagamentoFalhou: serviceOrderId={}, reason={}", os, reason);
        publisher.publish(MessageEnvelope.of("PagamentoFalhou", os, os,
                Map.of("reason", reason == null ? "pagamento recusado" : reason)));
    }
}
