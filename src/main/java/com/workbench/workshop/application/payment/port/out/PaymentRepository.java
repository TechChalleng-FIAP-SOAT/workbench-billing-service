package com.workbench.workshop.application.payment.port.out;

import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.domain.payment.PaymentHistory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findByServiceOrderId(UUID serviceOrderId);
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    PaymentHistory appendHistory(PaymentHistory history);
    List<PaymentHistory> findHistory(UUID serviceOrderId);
}
