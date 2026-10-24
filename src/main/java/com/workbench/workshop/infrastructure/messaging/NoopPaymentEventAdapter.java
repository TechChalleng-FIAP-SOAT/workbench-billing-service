package com.workbench.workshop.infrastructure.messaging;

import com.workbench.workshop.application.payment.port.out.PaymentEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback sem broker ({@code messaging.enabled=false}, default): apenas loga.
 * Mantém o Billing funcional em dev/testes sem LocalStack/AWS.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class NoopPaymentEventAdapter implements PaymentEventPort {

    @Override
    public void paymentConfirmed(UUID serviceOrderId) {
        log.info("[noop] PagamentoConfirmado não publicado (messaging desligado): serviceOrderId={}", serviceOrderId);
    }

    @Override
    public void paymentFailed(UUID serviceOrderId, String reason) {
        log.info("[noop] PagamentoFalhou não publicado (messaging desligado): serviceOrderId={}, reason={}",
                serviceOrderId, reason);
    }
}
