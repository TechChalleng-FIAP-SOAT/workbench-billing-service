package com.workbench.workshop.application.payment;

import com.workbench.workshop.application.Requester;
import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.application.payment.port.out.PaymentEventPort;
import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
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
    private PaymentEventPort paymentEventPort;
    private CreatePaymentUseCase createPayment;
    private HandlePaymentNotificationUseCase handleNotification;
    private ReconcilePaymentUseCase reconcilePayment;
    private FindPaymentByServiceOrderUseCase findPaymentByServiceOrder;
    private FindPaymentHistoryUseCase findPaymentHistory;
    private RefundPaymentUseCase refundPayment;

    private final UUID osId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        budgetRepository = mock(BudgetRepository.class);
        gateway = mock(PaymentGatewayPort.class);
        paymentEventPort = mock(PaymentEventPort.class);
        var recorder = new PaymentDecisionRecorder(paymentRepository, paymentEventPort);
        createPayment = new CreatePaymentUseCase(paymentRepository, budgetRepository, gateway);
        handleNotification = new HandlePaymentNotificationUseCase(paymentRepository, recorder);
        reconcilePayment = new ReconcilePaymentUseCase(gateway, recorder);
        findPaymentByServiceOrder = new FindPaymentByServiceOrderUseCase(paymentRepository, budgetRepository);
        findPaymentHistory = new FindPaymentHistoryUseCase(paymentRepository, budgetRepository);
        refundPayment = new RefundPaymentUseCase(paymentRepository, gateway);
        when(budgetRepository.ownerOf(any())).thenCallRealMethod();
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Budget budget() {
        return budgetOwnedBy(null);
    }

    private Budget budgetOwnedBy(String customerDocument) {
        return Budget.reconstitute(1L, osId, customerDocument, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("150.00"), List.of(), null, null);
    }

    @Test
    void createPaymentChargesGatewayAndRecordsPending() {
        when(budgetRepository.lockByServiceOrderId(osId)).thenReturn(Optional.of(budget()));
        when(gateway.createCharge(eq(osId.toString()), any()))
                .thenReturn(new PaymentGatewayPort.GatewayCharge("GW-99", "http://checkout"));

        Payment p = createPayment.execute(osId);

        assertEquals(PaymentStatus.PENDENTE, p.getStatus());
        assertEquals("GW-99", p.getGatewayPaymentId());
        verify(paymentRepository).appendHistory(any());
    }

    @Test
    void createPaymentIsIdempotent() {
        // Comando repetido: devolve o pagamento existente e não cria 2ª cobrança no gateway.
        Payment existing = Payment.create(osId, 1L, new BigDecimal("150.00"), "GW-99", "http://checkout");
        when(paymentRepository.findByServiceOrderId(osId)).thenReturn(Optional.of(existing));

        Payment p = createPayment.execute(osId);

        assertSame(existing, p);
        verifyNoInteractions(gateway);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void repeatedRejectionIsIdempotent() {
        Payment rejected = Payment.create(osId, 1L, new BigDecimal("150.00"), "GW-99", "http://checkout");
        rejected.reject();
        when(paymentRepository.findByGatewayPaymentId("GW-99")).thenReturn(Optional.of(rejected));

        Payment result = handleNotification.execute("GW-99", false);

        assertEquals(PaymentStatus.RECUSADO, result.getStatus());
        verifyNoInteractions(paymentEventPort);
    }

    @Test
    void clientCannotSeeAnotherClientsPayment() {
        Budget b = budgetOwnedBy("52998224725");
        when(budgetRepository.findByServiceOrderId(osId)).thenReturn(Optional.of(b));

        assertThrows(SecurityException.class,
                () -> findPaymentByServiceOrder.execute(osId, new Requester("11144477735", true)));
        assertThrows(SecurityException.class,
                () -> findPaymentHistory.execute(osId, new Requester("11144477735", true)));
    }

    @Test
    void clientSeesOwnPayment() {
        Budget b = budgetOwnedBy("52998224725");
        when(budgetRepository.findByServiceOrderId(osId)).thenReturn(Optional.of(b));
        when(paymentRepository.findByServiceOrderId(osId)).thenReturn(Optional.empty());

        assertTrue(findPaymentByServiceOrder.execute(osId, new Requester("529.982.247-25", true)).isEmpty());
    }

    @Test
    void createPaymentFailsWithoutBudget() {
        when(budgetRepository.lockByServiceOrderId(osId)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> createPayment.execute(osId));
    }

    @Test
    void webhookApprovesPayment() {
        Payment pending = Payment.create(osId, 1L, new BigDecimal("150.00"), "GW-99", "http://checkout/GW-99");
        when(paymentRepository.findByGatewayPaymentId("GW-99")).thenReturn(Optional.of(pending));

        Payment result = handleNotification.execute("GW-99", true);

        assertEquals(PaymentStatus.PAGAMENTO_APROVADO, result.getStatus());
        verify(paymentRepository).appendHistory(any());
        verify(paymentEventPort).paymentConfirmed(osId);
    }

    @Test
    void webhookRejectsPayment() {
        Payment pending = Payment.create(osId, 1L, new BigDecimal("150.00"), "GW-99", "http://checkout/GW-99");
        when(paymentRepository.findByGatewayPaymentId("GW-99")).thenReturn(Optional.of(pending));

        Payment result = handleNotification.execute("GW-99", false);

        assertEquals(PaymentStatus.RECUSADO, result.getStatus());
        verify(paymentEventPort).paymentFailed(eq(osId), any());
    }

    @Test
    void webhookApproveIsIdempotent() {
        Payment approved = Payment.create(osId, 1L, new BigDecimal("150.00"), "GW-99", "http://checkout/GW-99");
        approved.approve(null); // já aprovado
        when(paymentRepository.findByGatewayPaymentId("GW-99")).thenReturn(Optional.of(approved));

        // 2ª notificação de aprovação (reenvio do gateway): não falha e não reprocessa.
        Payment result = handleNotification.execute("GW-99", true);

        assertEquals(PaymentStatus.PAGAMENTO_APROVADO, result.getStatus());
        verify(paymentRepository, never()).save(any());
        verify(paymentRepository, never()).appendHistory(any());
        verify(paymentEventPort, never()).paymentConfirmed(any());
    }

    @Test
    void reconcileApprovesWhenGatewayApproved() {
        Payment pending = Payment.create(osId, 1L, new BigDecimal("150.00"), "PREF-1", "http://checkout");
        when(gateway.findPayment(osId.toString())).thenReturn(new PaymentGatewayPort.GatewayPaymentResult(PaymentGatewayPort.GatewayPaymentStatus.APPROVED, "MP-777"));

        Payment result = reconcilePayment.execute(pending);

        assertEquals(PaymentStatus.PAGAMENTO_APROVADO, result.getStatus());
        verify(paymentEventPort).paymentConfirmed(osId);
    }

    @Test
    void reconcileRejectsWhenGatewayRejected() {
        Payment pending = Payment.create(osId, 1L, new BigDecimal("150.00"), "PREF-1", "http://checkout");
        when(gateway.findPayment(osId.toString())).thenReturn(new PaymentGatewayPort.GatewayPaymentResult(PaymentGatewayPort.GatewayPaymentStatus.REJECTED, "MP-777"));

        Payment result = reconcilePayment.execute(pending);

        assertEquals(PaymentStatus.RECUSADO, result.getStatus());
        verify(paymentEventPort).paymentFailed(eq(osId), any());
    }

    @Test
    void reconcileKeepsPendingWithoutDecision() {
        Payment pending = Payment.create(osId, 1L, new BigDecimal("150.00"), "PREF-1", "http://checkout");
        when(gateway.findPayment(osId.toString())).thenReturn(new PaymentGatewayPort.GatewayPaymentResult(PaymentGatewayPort.GatewayPaymentStatus.PENDING, "MP-777"));

        Payment result = reconcilePayment.execute(pending);

        assertEquals(PaymentStatus.PENDENTE, result.getStatus());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(paymentEventPort);
    }

    @Test
    void refundsApprovedPayment() {
        Payment approved = Payment.create(osId, 1L, new BigDecimal("150.00"), "GW-99", "http://checkout/GW-99");
        approved.approve(null);
        when(paymentRepository.findByServiceOrderId(osId)).thenReturn(Optional.of(approved));

        Payment result = refundPayment.execute(osId);

        assertEquals(PaymentStatus.ESTORNADO, result.getStatus());
    }
}
