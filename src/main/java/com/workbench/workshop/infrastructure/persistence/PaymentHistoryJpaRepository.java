package com.workbench.workshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentHistoryJpaRepository extends JpaRepository<PaymentHistoryEntity, Long> {
    List<PaymentHistoryEntity> findByServiceOrderIdOrderByCreatedAtAsc(UUID serviceOrderId);
}
