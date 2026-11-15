package com.workbench.workshop.infrastructure.payment;

import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Gateway de pagamento FALSO ({@code payment.provider=fake}: perfil local e testes).
 *
 * <p>Determinístico e offline — usado em testes/dev e para demonstrar a Saga sem
 * depender do Mercado Pago. Gera um id de cobrança sintético; o resultado
 * (aprovado/recusado) é simulado chamando o webhook manualmente.
 */
@Component
@ConditionalOnProperty(prefix = "payment", name = "provider", havingValue = "fake")
@Slf4j
public class FakePaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public GatewayCharge createCharge(String chargeReference, BigDecimal amount) {
        String id = "FAKE-" + UUID.randomUUID();
        log.info("[FAKE gateway] cobrança criada: ref={}, amount={}, gatewayId={}", chargeReference, amount, id);
        return new GatewayCharge(id, "https://fake-checkout.local/" + id);
    }

    /** No fake a decisão vem do webhook simulado, nunca da consulta. */
    @Override
    public GatewayPaymentResult findPayment(String chargeReference) {
        return GatewayPaymentResult.pending();
    }

    @Override
    public void refund(String transactionId) {
        log.info("[FAKE gateway] estorno simulado: transactionId={}", transactionId);
    }
}
