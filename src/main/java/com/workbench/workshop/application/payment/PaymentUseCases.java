package com.workbench.workshop.application.payment;

import com.workbench.workshop.application.budget.port.out.BudgetRepository;
import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.domain.payment.PaymentHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Casos de uso de pagamento. O pagamento ocorre no fim do fluxo (ADR-0005):
 * criar cobrança (PENDENTE) → confirmação via webhook (APROVADO/RECUSADO) →
 * estorno (ESTORNADO) na compensação da Saga. Cada transição grava no histórico.
 */
@Service
@RequiredArgsConstructor
public class PaymentUseCases {

    private final PaymentRepository paymentRepository;
    private final BudgetRepository budgetRepository;
    private final PaymentGatewayPort gateway;

    /** Cria a cobrança no gateway e registra o pagamento PENDENTE. */
    @Transactional
    public Payment createPayment(UUID serviceOrderId) {
        Budget budget = budgetRepository.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new NoSuchElementException("Budget not found for OS: " + serviceOrderId));

        var charge = gateway.createCharge(serviceOrderId, budget.getTotalPrice());

        Payment payment = Payment.create(serviceOrderId, budget.getId(), budget.getTotalPrice(),
                charge.gatewayPaymentId());
        Payment saved = paymentRepository.save(payment);
        paymentRepository.appendHistory(PaymentHistory.of(saved.getId(), serviceOrderId,
                saved.getStatus(), "Cobrança criada no gateway: " + charge.gatewayPaymentId()));
        return saved;
    }

    /** Processa a notificação do gateway (webhook): aprova ou recusa. */
    @Transactional
    public Payment handleGatewayNotification(String gatewayPaymentId, boolean approved) {
        Payment payment = paymentRepository.findByGatewayPaymentId(gatewayPaymentId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Payment not found for gateway id: " + gatewayPaymentId));
        if (approved) {
            payment.approve();
        } else {
            payment.reject();
        }
        Payment saved = paymentRepository.save(payment);
        paymentRepository.appendHistory(PaymentHistory.of(saved.getId(), saved.getServiceOrderId(),
                saved.getStatus(), "Webhook do gateway: " + (approved ? "aprovado" : "recusado")));
        return saved;
    }

    /** Estorna um pagamento aprovado (compensação da Saga). */
    @Transactional
    public Payment refund(UUID serviceOrderId) {
        Payment payment = paymentRepository.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new NoSuchElementException("Payment not found for OS: " + serviceOrderId));
        payment.refund();
        Payment saved = paymentRepository.save(payment);
        paymentRepository.appendHistory(PaymentHistory.of(saved.getId(), serviceOrderId,
                saved.getStatus(), "Estorno (compensação da Saga)"));
        return saved;
    }
}
