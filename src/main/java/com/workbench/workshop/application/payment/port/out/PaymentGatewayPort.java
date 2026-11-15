package com.workbench.workshop.application.payment.port.out;

import java.math.BigDecimal;

/**
 * Abstração do provedor de pagamento (Mercado Pago). Permite trocar entre o
 * adapter fake (testes/dev), o sandbox e a produção sem mexer no domínio — a troca
 * é apenas de configuração (ADR-0005).
 */
public interface PaymentGatewayPort {

    /**
     * Cria uma cobrança (link de checkout) no provedor com a referência externa dada
     * ({@code Payment#chargeReference()}). A decisão vem depois por
     * {@link #findPayment(String)} (job de consulta) ou, no local, pelo webhook simulado.
     */
    GatewayCharge createCharge(String chargeReference, BigDecimal amount);

    /**
     * Consulta no provedor o pagamento da cobrança (pela referência externa).
     * {@link GatewayPaymentStatus#PENDING} quando ainda não há decisão.
     */
    GatewayPaymentResult findPayment(String chargeReference);

    /** Devolve ao cliente o valor de um pagamento aprovado (id da transação no provedor). */
    void refund(String transactionId);

    record GatewayCharge(String gatewayPaymentId, String checkoutUrl) {}

    /** Situação no provedor; {@code transactionId} é o id do pagamento aprovado (para estorno). */
    record GatewayPaymentResult(GatewayPaymentStatus status, String transactionId) {
        public static GatewayPaymentResult pending() {
            return new GatewayPaymentResult(GatewayPaymentStatus.PENDING, null);
        }
    }

    enum GatewayPaymentStatus { APPROVED, REJECTED, PENDING }
}
