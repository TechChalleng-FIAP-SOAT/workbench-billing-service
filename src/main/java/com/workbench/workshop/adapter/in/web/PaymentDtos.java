package com.workbench.workshop.adapter.in.web;

import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.domain.payment.PaymentHistory;

import java.math.BigDecimal;
import java.util.UUID;

/** DTOs de saída do pagamento e do histórico. */
public final class PaymentDtos {

    private PaymentDtos() {}

    public record PaymentResponse(Long id, UUID serviceOrderId, BigDecimal amount,
                                  String status, String gatewayPaymentId, String checkoutUrl) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getServiceOrderId(), p.getAmount(),
                    p.getStatus().name(), p.getGatewayPaymentId(), p.getCheckoutUrl());
        }
    }

    public record HistoryResponse(String status, String detail, String createdAt) {
        static HistoryResponse from(PaymentHistory h) {
            return new HistoryResponse(h.getStatus().name(), h.getDetail(),
                    h.getCreatedAt() == null ? null : h.getCreatedAt().toString());
        }
    }
}
