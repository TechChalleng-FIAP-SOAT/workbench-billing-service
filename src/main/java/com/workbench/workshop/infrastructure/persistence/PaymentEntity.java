package com.workbench.workshop.infrastructure.persistence;

import com.workbench.workshop.domain.payment.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "payments")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_seq")
    @SequenceGenerator(name = "payment_seq", sequenceName = "payment_seq", allocationSize = 50)
    private Long id;
    @Column(nullable = false, unique = true) private UUID serviceOrderId;
    private Long budgetId;
    @Column(nullable = false) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status;
    @Column(unique = true) private String gatewayPaymentId;
    @Column(length = 1000) private String checkoutUrl;
    @Column(updatable = false) private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); this.updatedAt = this.createdAt; }
    @PreUpdate protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
