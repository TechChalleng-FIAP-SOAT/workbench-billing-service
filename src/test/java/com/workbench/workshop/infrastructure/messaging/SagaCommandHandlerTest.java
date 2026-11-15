package com.workbench.workshop.infrastructure.messaging;

import com.workbench.workshop.application.budget.GenerateBudgetUseCase;
import com.workbench.workshop.application.payment.CreatePaymentUseCase;
import com.workbench.workshop.application.payment.RefundPaymentUseCase;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.payment.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SagaCommandHandlerTest {

    private GenerateBudgetUseCase generateBudgetUseCase;
    private CreatePaymentUseCase createPayment;
    private RefundPaymentUseCase refundPayment;
    private BillingEventPublisher publisher;
    private SagaCommandHandler handler;

    private final UUID osId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        generateBudgetUseCase = mock(GenerateBudgetUseCase.class);
        createPayment = mock(CreatePaymentUseCase.class);
        refundPayment = mock(RefundPaymentUseCase.class);
        publisher = mock(BillingEventPublisher.class);
        handler = new SagaCommandHandler(generateBudgetUseCase, createPayment, refundPayment, publisher);
    }

    private MessageEnvelope cmd(String type, Map<String, Object> payload) {
        return new MessageEnvelope("m1", "saga-1", osId.toString(), type, "2026-01-01T00:00:00Z", payload);
    }

    @Test
    void generateBudgetPublishesBudgetGenerated() {
        Budget b = Budget.reconstitute(10L, osId, "52998224725", new BigDecimal("100.00"),
                new BigDecimal("200.00"), new BigDecimal("300.00"), List.of(), null, null);
        when(generateBudgetUseCase.execute(eq(osId), any(), any(), any())).thenReturn(b);

        var payload = Map.<String, Object>of(
                "parts", List.of(Map.of(
                        "productId", "1", "name", "Pastilha", "sku", "PF-1", "brand", "Marca X",
                        "itemType", "PART", "unitPrice", "50.00", "quantity", "2")),
                "labor", List.of(Map.of(
                        "serviceName", "DISCO E PASTILHA", "laborCode", "00", "laborCost", "200.00")));
        handler.handle(cmd("ComandoGerarOrcamento", payload));

        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(publisher).publish(captor.capture());
        assertEquals("OrcamentoGerado", captor.getValue().type());
    }

    @Test
    void malformedPartRepliesBudgetFailed() {
        // itemType ausente: antes ia para a DLQ sem resposta; agora é OrcamentoFalhou.
        var payload = Map.<String, Object>of(
                "parts", List.of(Map.of("productId", "1", "unitPrice", "10", "quantity", "1")),
                "labor", List.of());
        handler.handle(cmd("ComandoGerarOrcamento", payload));

        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(publisher).publish(captor.capture());
        assertEquals("OrcamentoFalhou", captor.getValue().type());
        verifyNoInteractions(generateBudgetUseCase);
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingOptionalFieldsStayNullInsteadOfTheTextNull() {
        // Produto fora do catálogo: o Execution manda a peça sem sku/brand.
        Budget b = Budget.reconstitute(10L, osId, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), null, null);
        when(generateBudgetUseCase.execute(eq(osId), any(), any(), any())).thenReturn(b);
        var payload = Map.<String, Object>of(
                "parts", List.of(Map.of("productId", "1", "name", "Produto 1", "itemType", "PART",
                        "unitPrice", "0", "quantity", "1")),
                "labor", List.of());

        handler.handle(cmd("ComandoGerarOrcamento", payload));

        ArgumentCaptor<List<GenerateBudgetUseCase.PartInput>> parts = ArgumentCaptor.forClass(List.class);
        verify(generateBudgetUseCase).execute(eq(osId), any(), parts.capture(), any());
        assertNull(parts.getValue().get(0).sku());
        assertNull(parts.getValue().get(0).brand());
    }

    @Test
    void missingRequiredFieldRepliesBudgetFailed() {
        var payload = Map.<String, Object>of(
                "parts", List.of(),
                "labor", List.of(Map.of("serviceName", "ALINHAMENTO"))); // sem laborCost
        handler.handle(cmd("ComandoGerarOrcamento", payload));

        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(publisher).publish(captor.capture());
        assertEquals("OrcamentoFalhou", captor.getValue().type());
        assertEquals("Campo obrigatório ausente no item de mão de obra: laborCost", captor.getValue().payload().get("reason"));
        verifyNoInteractions(generateBudgetUseCase);
    }

    @Test
    void generateBudgetPublishesBudgetFailedOnError() {
        when(generateBudgetUseCase.execute(eq(osId), any(), any(), any()))
                .thenThrow(new RuntimeException("itens inválidos"));

        var payload = Map.<String, Object>of("parts", List.of(), "labor", List.of());
        handler.handle(cmd("ComandoGerarOrcamento", payload));

        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(publisher).publish(captor.capture());
        assertEquals("OrcamentoFalhou", captor.getValue().type());
    }

    @Test
    void chargePaymentPublishesChargeCreated() {
        Payment p = Payment.create(osId, 1L, new BigDecimal("100.00"), "GW-1", "http://checkout/GW-1");
        when(createPayment.execute(osId, "saga-1")).thenReturn(p);

        handler.handle(cmd("ComandoCobrarPagamento", Map.of()));

        // A rodada do comando vai para a cobrança (volta no PagamentoConfirmado).
        verify(createPayment).execute(osId, "saga-1");
        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(publisher).publish(captor.capture());
        assertEquals("CobrancaCriada", captor.getValue().type());
    }

    @Test
    void refundPublishesPaymentRefunded() {
        Payment p = Payment.create(osId, 1L, new BigDecimal("100.00"), "GW-1", "http://checkout/GW-1");
        p.approve(null);
        p.refund();
        when(refundPayment.execute(osId)).thenReturn(p);

        handler.handle(cmd("ComandoEstornarPagamento", Map.of()));

        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(publisher).publish(captor.capture());
        assertEquals("PagamentoEstornado", captor.getValue().type());
    }

    @Test
    void unknownCommandIgnored() {
        handler.handle(cmd("ComandoDesconhecido", Map.of()));
        verify(publisher, never()).publish(any());
    }
}
