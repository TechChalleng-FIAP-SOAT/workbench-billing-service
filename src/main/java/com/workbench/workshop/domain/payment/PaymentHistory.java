package com.workbench.workshop.domain.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro append-only do histórico de pagamento (auditoria / "registro e
 * verificação de pagamentos" do PDF). Um registro por transição de status.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistory {
    private Long id;
    private Long paymentId;
    private UUID serviceOrderId;
    private PaymentStatus status;
    private String detail;
    private LocalDateTime createdAt;

    public static PaymentHistory of(Long paymentId, UUID serviceOrderId, PaymentStatus status, String detail) {
        PaymentHistory h = new PaymentHistory();
        h.paymentId = paymentId;
        h.serviceOrderId = serviceOrderId;
        h.status = status;
        h.detail = detail;
        return h;
    }
}
