package com.workbench.workshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByServiceOrderId(UUID serviceOrderId);
    Optional<PaymentEntity> findByGatewayPaymentId(String gatewayPaymentId);
}
