package com.workbench.workshop.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BudgetJpaRepository extends JpaRepository<BudgetEntity, Long> {
    Optional<BudgetEntity> findByServiceOrderId(UUID serviceOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BudgetEntity b where b.serviceOrderId = :serviceOrderId")
    Optional<BudgetEntity> lockByServiceOrderId(@Param("serviceOrderId") UUID serviceOrderId);
}
