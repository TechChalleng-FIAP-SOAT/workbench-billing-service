package com.workbench.workshop.domain.payment;

/**
 * Ciclo de vida do pagamento (ver ADR-0005):
 * PENDENTE (criado no gateway, aguardando) → APROVADO (confirmado via webhook)
 *                                           → RECUSADO (negado pelo gateway)
 * APROVADO → ESTORNADO (compensação da Saga).
 */
public enum PaymentStatus {
    PENDENTE, APROVADO, RECUSADO, ESTORNADO
}
