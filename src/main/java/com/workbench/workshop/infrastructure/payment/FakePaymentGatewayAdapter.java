package com.workbench.workshop.infrastructure.payment;

import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Gateway de pagamento FALSO (default, {@code payment.provider=fake}).
 *
 * <p>Determinístico e offline — usado em testes/dev e para demonstrar a Saga sem
 * depender do Mercado Pago. Gera um id de cobrança sintético; o resultado
 * (aprovado/recusado) é simulado chamando o webhook manualmente ou pela Saga.
 */
@Component
@ConditionalOnProperty(prefix = "payment", name = "provider", havingValue = "fake", matchIfMissing = true)
@Slf4j
public class FakePaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public GatewayCharge createCharge(UUID serviceOrderId, BigDecimal amount) {
        String id = "FAKE-" + UUID.randomUUID();
        log.info("[FAKE gateway] cobrança criada: os={}, amount={}, gatewayId={}", serviceOrderId, amount, id);
        return new GatewayCharge(id, "https://fake-checkout.local/" + id);
    }
}
