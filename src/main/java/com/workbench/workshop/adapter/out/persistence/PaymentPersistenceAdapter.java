package com.workbench.workshop.adapter.out.persistence;

import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.domain.payment.PaymentHistory;
import com.workbench.workshop.infrastructure.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentHistoryJpaRepository historyJpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = (payment.getId() != null)
                ? paymentJpaRepository.findById(payment.getId()).orElseGet(PaymentEntity::new)
                : new PaymentEntity();
        entity.setServiceOrderId(payment.getServiceOrderId());
        entity.setBudgetId(payment.getBudgetId());
        entity.setAmount(payment.getAmount());
        entity.setStatus(payment.getStatus());
        entity.setGatewayPaymentId(payment.getGatewayPaymentId());
        return toDomain(paymentJpaRepository.save(entity));
    }

    @Override
    public Optional<Payment> findByServiceOrderId(UUID serviceOrderId) {
        return paymentJpaRepository.findByServiceOrderId(serviceOrderId).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId) {
        return paymentJpaRepository.findByGatewayPaymentId(gatewayPaymentId).map(this::toDomain);
    }

    @Override
    public PaymentHistory appendHistory(PaymentHistory history) {
        PaymentHistoryEntity e = new PaymentHistoryEntity();
        e.setPaymentId(history.getPaymentId());
        e.setServiceOrderId(history.getServiceOrderId());
        e.setStatus(history.getStatus());
        e.setDetail(history.getDetail());
        PaymentHistoryEntity saved = historyJpaRepository.save(e);
        history.setId(saved.getId());
        history.setCreatedAt(saved.getCreatedAt());
        return history;
    }

    @Override
    public List<PaymentHistory> findHistory(UUID serviceOrderId) {
        return historyJpaRepository.findByServiceOrderIdOrderByCreatedAtAsc(serviceOrderId).stream()
                .map(h -> {
                    PaymentHistory ph = PaymentHistory.of(h.getPaymentId(), h.getServiceOrderId(), h.getStatus(), h.getDetail());
                    ph.setId(h.getId());
                    ph.setCreatedAt(h.getCreatedAt());
                    return ph;
                }).toList();
    }

    private Payment toDomain(PaymentEntity e) {
        return Payment.reconstitute(e.getId(), e.getServiceOrderId(), e.getBudgetId(), e.getAmount(),
                e.getStatus(), e.getGatewayPaymentId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
