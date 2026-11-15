package com.workbench.workshop.contract;

import com.workbench.workshop.application.budget.GenerateBudgetUseCase;
import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.application.payment.CreatePaymentUseCase;
import com.workbench.workshop.application.payment.RefundPaymentUseCase;
import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.infrastructure.messaging.BillingEventPublisher;
import com.workbench.workshop.infrastructure.messaging.MessageEnvelope;
import com.workbench.workshop.infrastructure.messaging.SagaCommandHandler;
import com.workbench.workshop.infrastructure.messaging.SagaMessages;
import com.workbench.workshop.infrastructure.messaging.SnsPaymentEventAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Contrato da Saga do lado do Billing: nomes de mensagem, o que ele CONSOME
 * (exemplo do contrato é aceito) e o que ele PUBLICA (campos obrigatórios presentes).
 */
class BillingSagaContractTest {

    private static final String SERVICE = "billing-service";
    private final SagaContract contract = SagaContract.load();
    private final UUID osId = UUID.randomUUID();

    private BudgetRepository budgetRepository;
    private CreatePaymentUseCase createPayment;
    private RefundPaymentUseCase refundPayment;
    private BillingEventPublisher publisher;
    private SagaCommandHandler handler;

    @BeforeEach
    void setUp() {
        budgetRepository = mock(BudgetRepository.class);
        when(budgetRepository.findByServiceOrderId(any())).thenReturn(java.util.Optional.empty());
        when(budgetRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        createPayment = mock(CreatePaymentUseCase.class);
        refundPayment = mock(RefundPaymentUseCase.class);
        publisher = mock(BillingEventPublisher.class);
        // GenerateBudgetUseCase real: o teste prova que o payload do contrato é lido de verdade.
        handler = new SagaCommandHandler(new GenerateBudgetUseCase(budgetRepository), createPayment, refundPayment, publisher);
    }

    @Test
    void messageNamesMatchContract() {
        var names = SagaContract.constantValues(SagaMessages.class);
        assertTrue(contract.typesInvolving(SERVICE).containsAll(names),
                "SagaMessages tem nome fora do contrato: " + names);
        assertTrue(names.containsAll(contract.typesImplementedBy(SERVICE)),
                "SagaMessages não cobre o contrato: " + contract.typesImplementedBy(SERVICE));
    }

    @Test
    void consumesGenerateBudgetAndPublishesBudgetGenerated() {
        handler.handle(command(SagaMessages.GENERATE_BUDGET));

        MessageEnvelope reply = lastPublished();
        assertEquals(SagaMessages.BUDGET_GENERATED, reply.type(), "exemplo do contrato foi rejeitado: " + reply.payload());
        assertConforms(reply);
    }

    @Test
    void budgetFailedConformsToContract() {
        handler.handle(MessageEnvelope.of(SagaMessages.GENERATE_BUDGET, osId.toString(), osId.toString(),
                Map.of("parts", List.of(Map.of("productId", "p-1")), "labor", List.of())));

        MessageEnvelope reply = lastPublished();
        assertEquals(SagaMessages.BUDGET_FAILED, reply.type());
        assertConforms(reply);
    }

    @Test
    void consumesChargePaymentAndPublishesChargeCreated() {
        when(createPayment.execute(osId)).thenReturn(
                Payment.create(osId, 1L, new BigDecimal("250.00"), "PREF-1", "https://checkout/PREF-1"));

        handler.handle(command(SagaMessages.CHARGE_PAYMENT));

        assertConforms(lastPublished());
    }

    @Test
    void consumesRefundAndPublishesPaymentRefunded() {
        Payment approved = Payment.create(osId, 1L, new BigDecimal("250.00"), "PREF-1", "https://checkout/PREF-1");
        approved.approve(null);
        approved.refund();
        when(refundPayment.execute(osId)).thenReturn(approved);

        handler.handle(command(SagaMessages.REFUND_PAYMENT));

        assertConforms(lastPublished());
    }

    @Test
    void paymentEventsConformToContract() {
        var adapter = new SnsPaymentEventAdapter(publisher);

        adapter.paymentConfirmed(osId);
        assertConforms(lastPublished());

        adapter.paymentFailed(osId, "pagamento recusado pelo gateway");
        assertConforms(lastPublished());
    }

    private MessageEnvelope command(String type) {
        return MessageEnvelope.of(type, osId.toString(), osId.toString(), contract.example(type));
    }

    private MessageEnvelope lastPublished() {
        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(publisher, atLeastOnce()).publish(captor.capture());
        return captor.getValue();
    }

    private void assertConforms(MessageEnvelope message) {
        assertEquals(List.of(), contract.missingFields(message.type(), message.payload()),
                "campos obrigatórios ausentes em " + message.type());
    }
}
