package com.workbench.workshop.infrastructure.payment;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.workbench.workshop.application.payment.port.out.PaymentGatewayPort.GatewayPaymentStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MercadoPagoGatewayAdapterTest {

    private static Map<String, Object> attempt(String id, String status) {
        return Map.of("id", id, "status", status);
    }

    @Test
    void noPaymentYetIsPending() {
        assertEquals(PENDING, MercadoPagoGatewayAdapter.toResult(List.of()).status());
    }

    @Test
    void anyApprovedAttemptApprovesWithItsTransactionId() {
        // Cliente errou o cartão e pagou na 2ª tentativa (mais recente primeiro).
        var result = MercadoPagoGatewayAdapter.toResult(List.of(attempt("222", "approved"), attempt("111", "rejected")));
        assertEquals(APPROVED, result.status());
        assertEquals("222", result.transactionId());
    }

    @Test
    void rejectedAttemptKeepsWaitingForRetry() {
        assertEquals(PENDING, MercadoPagoGatewayAdapter.toResult(List.of(attempt("1", "rejected"))).status());
        assertEquals(PENDING, MercadoPagoGatewayAdapter.toResult(List.of(attempt("1", "in_process"))).status());
    }

    @Test
    void cancelledLatestRejects() {
        var result = MercadoPagoGatewayAdapter.toResult(List.of(attempt("2", "cancelled"), attempt("1", "rejected")));
        assertEquals(REJECTED, result.status());
        assertNull(result.transactionId());
    }
}
