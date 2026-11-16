package com.workbench.workshop.domain.payment;

/**
 * Ciclo de vida do pagamento (ver ADR-0005):
 * PENDENTE (criado no gateway, aguardando) → PAGAMENTO_APROVADO (confirmado via webhook)
 *                                           → RECUSADO (negado pelo gateway)
 * PAGAMENTO_APROVADO → ESTORNADO (compensação da Saga).
 */
public enum PaymentStatus {
    PENDENTE, PAGAMENTO_APROVADO, RECUSADO, ESTORNADO
}
