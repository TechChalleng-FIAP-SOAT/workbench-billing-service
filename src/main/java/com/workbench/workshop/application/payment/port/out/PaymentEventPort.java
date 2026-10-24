package com.workbench.workshop.application.payment.port.out;

import java.util.UUID;

/**
 * Porta de saída para notificar a Saga sobre o resultado do pagamento, recebido
 * via webhook do gateway. O os-service (orquestrador) consome esses eventos para
 * finalizar a OS (PagamentoConfirmado → PAGO/ENTREGUE) ou retê-la (PagamentoFalhou).
 *
 * <p>Implementações: SNS (quando {@code messaging.enabled=true}) ou no-op (dev/testes
 * sem broker). O {@code sagaId} é o id da OS (uma Saga por OS).
 */
public interface PaymentEventPort {

    /** Pagamento aprovado pelo gateway → publica {@code PagamentoConfirmado}. */
    void paymentConfirmed(UUID serviceOrderId);

    /** Pagamento recusado pelo gateway → publica {@code PagamentoFalhou}. */
    void paymentFailed(UUID serviceOrderId, String reason);
}
