package com.workbench.workshop.infrastructure.persistence;

import com.workbench.workshop.domain.payment.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "payment_history")
public class PaymentHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_history_seq")
    @SequenceGenerator(name = "payment_history_seq", sequenceName = "payment_history_seq", allocationSize = 50)
    private Long id;
    @Column(nullable = false) private Long paymentId;
    @Column(nullable = false) private UUID serviceOrderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status;
    private String detail;
    @Column(updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}
