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
import java.util.Optional;
import java.util.UUID;

/**
 * Cria a cobrança da OS no gateway (pagamento PENDENTE, ADR-0005).
 *
 * <ul>
 *   <li>Sem pagamento: cria a 1ª cobrança.</li>
 *   <li>Pagamento RECUSADO: cria uma <b>nova</b> cobrança (novo link, nova referência
 *       {@code <OS>-N}) e reabre o pagamento — o cliente pode pagar de novo.</li>
 *   <li>Qualquer outro estado: idempotente, devolve o existente sem chamar o gateway
 *       (o comando pode chegar repetido por reenvio/SQS).</li>
 * </ul>
 *
 * <p>A linha do orçamento fica travada durante a criação: duas réplicas com o mesmo
 * comando não criam duas preferências no Mercado Pago — a segunda espera e devolve a
 * cobrança da primeira.
 */
@Service
@RequiredArgsConstructor
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final BudgetRepository budgetRepository;
    private final PaymentGatewayPort gateway;

    /** Cobrança pedida pela API (sem Saga): o resultado sai com o id da OS como sagaId. */
    @Transactional
    public Payment execute(UUID serviceOrderId) {
        return execute(serviceOrderId, null);
    }

    /**
     * Cobrança pedida pela Saga ({@code ComandoCobrarPagamento}). O {@code sagaId} da rodada
     * fica gravado no pagamento e volta no resultado (PagamentoConfirmado/PagamentoFalhou).
     */
    @Transactional
    public Payment execute(UUID serviceOrderId, String sagaId) {
        // Trava antes de ler o pagamento: quem chega depois só lê depois do commit do primeiro.
        Optional<Budget> lockedBudget = budgetRepository.lockByServiceOrderId(serviceOrderId);
        Optional<Payment> existing = paymentRepository.findByServiceOrderId(serviceOrderId);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (payment.isRejected()) {
                return chargeAgain(payment, sagaId);
            }
            // Comando repetido (ou cobrança criada antes pela API): só registra a rodada.
            if (sagaId != null && !sagaId.equals(payment.getSagaId())) {
                payment.bindToSaga(sagaId);
                return paymentRepository.save(payment);
            }
            return payment;
        }
        Budget budget = lockedBudget
                .orElseThrow(() -> new NoSuchElementException("Orçamento não encontrado para a OS: " + serviceOrderId));

        var charge = gateway.createCharge(Payment.chargeReference(serviceOrderId, 1), budget.getTotalPrice());
        Payment payment = Payment.create(serviceOrderId, budget.getId(),
                budget.getTotalPrice(), charge.gatewayPaymentId(), charge.checkoutUrl());
        payment.bindToSaga(sagaId);
        Payment saved = paymentRepository.save(payment);
        paymentRepository.appendHistory(PaymentHistory.of(saved.getId(), serviceOrderId,
                saved.getStatus(), "Cobrança criada no gateway: " + charge.gatewayPaymentId()));
        return saved;
    }

    private Payment chargeAgain(Payment rejected, String sagaId) {
        var charge = gateway.createCharge(rejected.nextChargeReference(), rejected.getAmount());
        rejected.reopen(charge.gatewayPaymentId(), charge.checkoutUrl());
        rejected.bindToSaga(sagaId);
        Payment saved = paymentRepository.save(rejected);
        paymentRepository.appendHistory(PaymentHistory.of(saved.getId(), saved.getServiceOrderId(),
                saved.getStatus(), "Nova cobrança após recusa (tentativa " + saved.getAttempt() + "): "
                        + charge.gatewayPaymentId()));
        return saved;
    }
}
