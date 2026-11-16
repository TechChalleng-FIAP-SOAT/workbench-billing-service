package com.workbench.workshop.infrastructure.payment;

import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FakePaymentGatewayAdapterTest {

    private final FakePaymentGatewayAdapter adapter = new FakePaymentGatewayAdapter();

    @Test
    void createChargeReturnsSyntheticId() {
        PaymentGatewayPort.GatewayCharge charge =
                adapter.createCharge(UUID.randomUUID().toString(), new BigDecimal("99.90"));

        assertNotNull(charge.gatewayPaymentId());
        assertTrue(charge.gatewayPaymentId().startsWith("FAKE-"));
        assertNotNull(charge.checkoutUrl());
    }

    @Test
    void findPaymentIsAlwaysPending() {
        // No fake, quem decide é o webhook simulado.
        assertEquals(PaymentGatewayPort.GatewayPaymentStatus.PENDING,
                adapter.findPayment(UUID.randomUUID().toString()).status());
    }
}
