package com.workbench.workshop.domain.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    private Payment pending() {
        return Payment.create(UUID.randomUUID(), 1L, new BigDecimal("100.00"), "GW-1", "http://checkout/GW-1");
    }

    @Test
    void createStartsPendente() {
        Payment p = pending();
        assertEquals(PaymentStatus.PENDENTE, p.getStatus());
        assertEquals(new BigDecimal("100.00"), p.getAmount());
    }

    @Test
    void createRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> Payment.create(UUID.randomUUID(), 1L, BigDecimal.ZERO, "GW", "http://checkout/GW"));
    }

    @Test
    void approveFromPendente() {
        Payment p = pending();
        p.approve();
        assertEquals(PaymentStatus.PAGAMENTO_APROVADO, p.getStatus());
    }

    @Test
    void rejectFromPendente() {
        Payment p = pending();
        p.reject();
        assertEquals(PaymentStatus.RECUSADO, p.getStatus());
    }

    @Test
    void refundOnlyFromApproved() {
        Payment p = pending();
        p.approve();
        p.refund();
        assertEquals(PaymentStatus.ESTORNADO, p.getStatus());
    }

    @Test
    void cannotApproveTwice() {
        Payment p = pending();
        p.approve();
        assertThrows(InvalidPaymentTransitionException.class, p::approve);
    }

    @Test
    void cannotRefundPendente() {
        Payment p = pending();
        assertThrows(InvalidPaymentTransitionException.class, p::refund);
    }
}
