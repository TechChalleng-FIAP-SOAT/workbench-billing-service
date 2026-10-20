package com.workbench.workshop.application.payment;

import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentUseCasesTest {

    private PaymentRepository paymentRepository;
    private BudgetRepository budgetRepository;
    private PaymentGatewayPort gateway;
    private PaymentUseCases useCases;

    private final UUID osId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        budgetRepository = mock(BudgetRepository.class);
        gateway = mock(PaymentGatewayPort.class);
        useCases = new PaymentUseCases(paymentRepository, budgetRepository, gateway);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Budget budget() {
        Budget b = new Budget();
        b.setId(1L);
        b.setServiceOrderId(osId);
        b.setTotalPrice(new BigDecimal("150.00"));
        return b;
    }

    @Test
    void createPaymentChargesGatewayAndRecordsPending() {
        when(budgetRepository.findByServiceOrderId(osId)).thenReturn(Optional.of(budget()));
        when(gateway.createCharge(eq(osId), any()))
                .thenReturn(new PaymentGatewayPort.GatewayCharge("GW-99", "http://checkout"));

        Payment p = useCases.createPayment(osId);

        assertEquals(PaymentStatus.PENDENTE, p.getStatus());
        assertEquals("GW-99", p.getGatewayPaymentId());
        verify(paymentRepository).appendHistory(any());
    }

    @Test
    void createPaymentFailsWithoutBudget() {
        when(budgetRepository.findByServiceOrderId(osId)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> useCases.createPayment(osId));
    }

    @Test
    void webhookApprovesPayment() {
        Payment pending = Payment.create(osId, 1L, new BigDecimal("150.00"), "GW-99");
        when(paymentRepository.findByGatewayPaymentId("GW-99")).thenReturn(Optional.of(pending));

        Payment result = useCases.handleGatewayNotification("GW-99", true);

        assertEquals(PaymentStatus.APROVADO, result.getStatus());
        verify(paymentRepository).appendHistory(any());
    }

    @Test
    void webhookRejectsPayment() {
        Payment pending = Payment.create(osId, 1L, new BigDecimal("150.00"), "GW-99");
        when(paymentRepository.findByGatewayPaymentId("GW-99")).thenReturn(Optional.of(pending));

        Payment result = useCases.handleGatewayNotification("GW-99", false);

        assertEquals(PaymentStatus.RECUSADO, result.getStatus());
    }

    @Test
    void refundEstornaPagamentoAprovado() {
        Payment approved = Payment.create(osId, 1L, new BigDecimal("150.00"), "GW-99");
        approved.approve();
        when(paymentRepository.findByServiceOrderId(osId)).thenReturn(Optional.of(approved));

        Payment result = useCases.refund(osId);

        assertEquals(PaymentStatus.ESTORNADO, result.getStatus());
    }
}
