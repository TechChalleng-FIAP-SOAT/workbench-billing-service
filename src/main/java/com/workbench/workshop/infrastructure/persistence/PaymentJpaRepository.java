package com.workbench.workshop.infrastructure.persistence;

import com.workbench.workshop.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByServiceOrderId(UUID serviceOrderId);
    Optional<PaymentEntity> findByGatewayPaymentId(String gatewayPaymentId);
    List<PaymentEntity> findByStatus(PaymentStatus status);
}
