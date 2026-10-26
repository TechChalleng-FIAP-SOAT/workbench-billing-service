package com.workbench.workshop.infrastructure.messaging;

import com.workbench.workshop.application.budget.GenerateBudgetUseCase;
import com.workbench.workshop.application.payment.PaymentUseCases;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SagaCommandHandlerTest {

    private GenerateBudgetUseCase generateBudgetUseCase;
    private PaymentUseCases paymentUseCases;
    private BillingEventPublisher publisher;
    private SagaCommandHandler handler;

    private final UUID osId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        generateBudgetUseCase = mock(GenerateBudgetUseCase.class);
        paymentUseCases = mock(PaymentUseCases.class);
        publisher = mock(BillingEventPublisher.class);
        handler = new SagaCommandHandler(generateBudgetUseCase, paymentUseCases, publisher);
    }

    private MessageEnvelope cmd(String type, Map<String, Object> payload) {
        return new MessageEnvelope("m1", "saga-1", osId.toString(), type, "2026-01-01T00:00:00Z", payload);
    }

    @Test
    void gerarOrcamentoPublishesOrcamentoGerado() {
        Budget b = new Budget();
        b.setId(10L);
        b.setTotalParts(new BigDecimal("100.00"));
        b.setTotalLabor(new BigDecimal("200.00"));
        b.setTotalPrice(new BigDecimal("300.00"));
        when(generateBudgetUseCase.execute(eq(osId), any(), any())).thenReturn(b);

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
    void gerarOrcamentoPublishesOrcamentoFalhouOnError() {
        when(generateBudgetUseCase.execute(eq(osId), any(), any()))
                .thenThrow(new RuntimeException("itens inválidos"));

        var payload = Map.<String, Object>of("parts", List.of(), "labor", List.of());
        handler.handle(cmd("ComandoGerarOrcamento", payload));

        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(publisher).publish(captor.capture());
        assertEquals("OrcamentoFalhou", captor.getValue().type());
    }

    @Test
    void cobrarPagamentoPublishesCobrancaCriada() {
        Payment p = Payment.create(osId, 1L, new BigDecimal("100.00"), "GW-1", "http://checkout/GW-1");
        when(paymentUseCases.createPayment(osId)).thenReturn(p);

        handler.handle(cmd("ComandoCobrarPagamento", Map.of()));

        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(publisher).publish(captor.capture());
        assertEquals("CobrancaCriada", captor.getValue().type());
    }

    @Test
    void estornarPublishesPagamentoEstornado() {
        Payment p = Payment.create(osId, 1L, new BigDecimal("100.00"), "GW-1", "http://checkout/GW-1");
        p.approve();
        p.refund();
        when(paymentUseCases.refund(osId)).thenReturn(p);

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
