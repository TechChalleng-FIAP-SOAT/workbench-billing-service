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
 * PENDENTE → PAGAMENTO_APROVADO | RECUSADO; RECUSADO → PENDENTE (nova cobrança);
 * PAGAMENTO_APROVADO → ESTORNADO. Transições inválidas lançam
 * {@link InvalidPaymentTransitionException}.
 *
 * <p>Cada cobrança tem uma referência própria no provedor ({@link #chargeReference()}):
 * a 1ª é o id da OS; uma nova cobrança após recusa vira {@code <OS>-2}, {@code <OS>-3}…,
 * para a consulta não confundir a tentativa nova com a recusada.
 */
@Getter
public class Payment {

    private Long id;
    private UUID serviceOrderId;
    private Long budgetId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String gatewayPaymentId;      // id da cobrança (preferência do Checkout Pro)
    private String checkoutUrl;           // link de pagamento para o cliente
    private String gatewayTransactionId;  // id do pagamento aprovado no provedor (usado no estorno)
    private int attempt = 1;              // nº da cobrança (sobe a cada nova cobrança após recusa)
    private String sagaId;                // rodada da Saga que pediu a cobrança (nulo = id da OS)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Payment() {}

    public static Payment create(UUID serviceOrderId, Long budgetId, BigDecimal amount,
                                 String gatewayPaymentId, String checkoutUrl) {
        Objects.requireNonNull(serviceOrderId, "Ordem de serviço é obrigatória");
        Objects.requireNonNull(amount, "Valor é obrigatório");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo");
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
                                       String gatewayTransactionId, int attempt,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        Payment p = new Payment();
        p.id = id;
        p.serviceOrderId = serviceOrderId;
        p.budgetId = budgetId;
        p.amount = amount;
        p.status = status;
        p.gatewayPaymentId = gatewayPaymentId;
        p.checkoutUrl = checkoutUrl;
        p.gatewayTransactionId = gatewayTransactionId;
        p.attempt = attempt;
        p.createdAt = createdAt;
        p.updatedAt = updatedAt;
        return p;
    }

    /** Referência da cobrança no provedor: id da OS na 1ª tentativa, {@code <OS>-N} nas seguintes. */
    public String chargeReference() {
        return chargeReference(serviceOrderId, attempt);
    }

    /** Referência que a próxima cobrança desta OS terá (antes de reabrir). */
    public String nextChargeReference() {
        return chargeReference(serviceOrderId, attempt + 1);
    }

    public static String chargeReference(UUID serviceOrderId, int attempt) {
        return attempt <= 1 ? serviceOrderId.toString() : serviceOrderId + "-" + attempt;
    }

    /** Aprova; {@code transactionId} é o id do pagamento no provedor (nulo no fake/webhook simulado). */
    public void approve(String transactionId) {
        requireStatus(PaymentStatus.PENDENTE, PaymentStatus.PAGAMENTO_APROVADO);
        this.status = PaymentStatus.PAGAMENTO_APROVADO;
        this.gatewayTransactionId = transactionId;
    }

    /** True quando já está aprovado (usado para idempotência). */
    public boolean isApproved() {
        return this.status == PaymentStatus.PAGAMENTO_APROVADO;
    }

    /** True quando já está recusado (usado para idempotência). */
    public boolean isRejected() {
        return this.status == PaymentStatus.RECUSADO;
    }

    public void reject() {
        requireStatus(PaymentStatus.PENDENTE, PaymentStatus.RECUSADO);
        this.status = PaymentStatus.RECUSADO;
    }

    /** Nova cobrança depois de uma recusa: volta a PENDENTE com o novo link. */
    public void reopen(String newGatewayPaymentId, String newCheckoutUrl) {
        requireStatus(PaymentStatus.RECUSADO, PaymentStatus.PENDENTE);
        this.status = PaymentStatus.PENDENTE;
        this.gatewayPaymentId = newGatewayPaymentId;
        this.checkoutUrl = newCheckoutUrl;
        this.attempt++;
    }

    /**
     * Liga a cobrança à rodada da Saga que a pediu ({@code sagaId} do ComandoCobrarPagamento).
     * Vazio não muda nada.
     */
    public void bindToSaga(String sagaId) {
        if (sagaId != null && !sagaId.isBlank()) {
            this.sagaId = sagaId;
        }
    }

    /**
     * {@code sagaId} a usar no resultado do pagamento. O orquestrador descarta resposta de
     * outra rodada; sem rodada gravada (cobrança antiga ou criada pela API), é o id da OS.
     */
    public String sagaIdForEvents() {
        return sagaId != null ? sagaId : serviceOrderId.toString();
    }

    public void refund() {
        requireStatus(PaymentStatus.PAGAMENTO_APROVADO, PaymentStatus.ESTORNADO);
        this.status = PaymentStatus.ESTORNADO;
    }

    private void requireStatus(PaymentStatus expected, PaymentStatus target) {
        if (this.status != expected) {
            throw new InvalidPaymentTransitionException(
                    "Não é possível mudar o pagamento de " + this.status + " para " + target + " (esperado " + expected + ")");
        }
    }
}
