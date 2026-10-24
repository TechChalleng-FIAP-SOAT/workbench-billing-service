package com.workbench.workshop.domain.payment;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Pagamento de uma OS (rich domain com máquina de estados).
 *
 * <p>Transições válidas:
 * PENDENTE → PAGAMENTO_APROVADO | RECUSADO; PAGAMENTO_APROVADO → ESTORNADO.
 * Transições inválidas lançam {@link InvalidPaymentTransitionException}.
 */
@Getter
public class Payment {

    private Long id;
    private UUID serviceOrderId;
    private Long budgetId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String gatewayPaymentId;   // id do pagamento no provedor (Mercado Pago)
    private String checkoutUrl;        // link de pagamento (checkout) para o cliente pagar
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Payment() {}

    public static Payment create(UUID serviceOrderId, Long budgetId, BigDecimal amount,
                                 String gatewayPaymentId, String checkoutUrl) {
        Objects.requireNonNull(serviceOrderId, "serviceOrderId is required");
        Objects.requireNonNull(amount, "amount is required");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        Payment p = new Payment();
        p.serviceOrderId = serviceOrderId;
        p.budgetId = budgetId;
        p.amount = amount;
        p.gatewayPaymentId = gatewayPaymentId;
        p.checkoutUrl = checkoutUrl;
        p.status = PaymentStatus.PENDENTE;
        return p;
    }

    public static Payment reconstitute(Long id, UUID serviceOrderId, Long budgetId, BigDecimal amount,
                                       PaymentStatus status, String gatewayPaymentId, String checkoutUrl,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        Payment p = new Payment();
        p.id = id;
        p.serviceOrderId = serviceOrderId;
        p.budgetId = budgetId;
        p.amount = amount;
        p.status = status;
        p.gatewayPaymentId = gatewayPaymentId;
        p.checkoutUrl = checkoutUrl;
        p.createdAt = createdAt;
        p.updatedAt = updatedAt;
        return p;
    }

    public void approve() {
        requireStatus(PaymentStatus.PENDENTE, PaymentStatus.PAGAMENTO_APROVADO);
        this.status = PaymentStatus.PAGAMENTO_APROVADO;
    }

    /** True quando já está aprovado (usado para idempotência do webhook). */
    public boolean isApproved() {
        return this.status == PaymentStatus.PAGAMENTO_APROVADO;
    }

    public void reject() {
        requireStatus(PaymentStatus.PENDENTE, PaymentStatus.RECUSADO);
        this.status = PaymentStatus.RECUSADO;
    }

    public void refund() {
        requireStatus(PaymentStatus.PAGAMENTO_APROVADO, PaymentStatus.ESTORNADO);
        this.status = PaymentStatus.ESTORNADO;
    }

    private void requireStatus(PaymentStatus expected, PaymentStatus target) {
        if (this.status != expected) {
            throw new InvalidPaymentTransitionException(
                    "Cannot transition from " + this.status + " to " + target + " (expected " + expected + ")");
        }
    }
}
