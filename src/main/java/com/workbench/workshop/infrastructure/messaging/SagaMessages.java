package com.workbench.workshop.infrastructure.messaging;

/**
 * Nomes dos comandos e eventos da Saga tratados pelo Billing (contrato com o
 * orquestrador do OS Service — mesmo nome no {@code SagaMessages} de lá).
 */
public final class SagaMessages {

    // Comandos recebidos
    public static final String GENERATE_BUDGET = "ComandoGerarOrcamento";
    public static final String CHARGE_PAYMENT = "ComandoCobrarPagamento";
    public static final String REFUND_PAYMENT = "ComandoEstornarPagamento";

    // Eventos publicados
    public static final String BUDGET_GENERATED = "OrcamentoGerado";
    public static final String BUDGET_FAILED = "OrcamentoFalhou";
    public static final String CHARGE_CREATED = "CobrancaCriada";
    public static final String PAYMENT_REFUNDED = "PagamentoEstornado";
    public static final String PAYMENT_CONFIRMED = "PagamentoConfirmado";
    public static final String PAYMENT_FAILED = "PagamentoFalhou";

    private SagaMessages() {}
}
