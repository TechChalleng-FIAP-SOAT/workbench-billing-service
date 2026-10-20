package com.workbench.workshop.adapter.in.web;

import com.workbench.workshop.application.payment.PaymentUseCases;
import com.workbench.workshop.application.payment.port.out.PaymentRepository;
import com.workbench.workshop.domain.payment.Payment;
import com.workbench.workshop.domain.payment.PaymentHistory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Pagamento (Mercado Pago) e histórico")
public class PaymentController {

    private final PaymentUseCases paymentUseCases;
    private final PaymentRepository paymentRepository;

    @PostMapping("/service-order/{serviceOrderId}")
    @Operation(summary = "Criar cobrança para a OS (pagamento pendente)")
    public ResponseEntity<PaymentResponse> create(@PathVariable UUID serviceOrderId) {
        Payment p = paymentUseCases.createPayment(serviceOrderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(p));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Webhook do gateway (notificação de aprovação/recusa)")
    public ResponseEntity<Void> webhook(@RequestBody WebhookPayload payload) {
        paymentUseCases.handleGatewayNotification(payload.gatewayPaymentId(), payload.approved());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/service-order/{serviceOrderId}/history")
    @Operation(summary = "Histórico de pagamento da OS")
    public ResponseEntity<List<HistoryResponse>> history(@PathVariable UUID serviceOrderId) {
        return ResponseEntity.ok(paymentRepository.findHistory(serviceOrderId).stream()
                .map(HistoryResponse::from).toList());
    }

    public record WebhookPayload(String gatewayPaymentId, boolean approved) {}

    public record PaymentResponse(Long id, UUID serviceOrderId, BigDecimal amount,
                                  String status, String gatewayPaymentId) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getServiceOrderId(), p.getAmount(),
                    p.getStatus().name(), p.getGatewayPaymentId());
        }
    }

    public record HistoryResponse(String status, String detail, String createdAt) {
        static HistoryResponse from(PaymentHistory h) {
            return new HistoryResponse(h.getStatus().name(), h.getDetail(),
                    h.getCreatedAt() == null ? null : h.getCreatedAt().toString());
        }
    }
}
