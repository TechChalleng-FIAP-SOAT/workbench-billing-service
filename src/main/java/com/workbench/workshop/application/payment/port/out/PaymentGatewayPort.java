package com.workbench.workshop.application.payment.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Abstração do provedor de pagamento (Mercado Pago). Permite trocar entre o
 * adapter fake (testes/dev), o sandbox e a produção sem mexer no domínio — a troca
 * é apenas de configuração (ADR-0005).
 */
public interface PaymentGatewayPort {

    /**
     * Cria uma cobrança no provedor e retorna o id externo do pagamento.
     * O resultado final (aprovado/recusado) chega depois via webhook.
     */
    GatewayCharge createCharge(UUID serviceOrderId, BigDecimal amount);

    record GatewayCharge(String gatewayPaymentId, String checkoutUrl) {}
}
