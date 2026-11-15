package com.workbench.workshop.adapter.in.web;

import com.workbench.workshop.adapter.in.web.FakePaymentWebhookDtos.WebhookPayload;
import com.workbench.workshop.application.payment.HandlePaymentNotificationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook SIMULADO de pagamento — só existe com o gateway fake
 * ({@code payment.provider=fake}, ambiente local/testes). Não verifica assinatura, por
 * isso nunca sobe com o Mercado Pago real: lá a confirmação vem do job de consulta
 * ({@code PaymentStatusPollingJob}).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment", name = "provider", havingValue = "fake", matchIfMissing = true)
@Tag(name = "Payment", description = "Pagamento (Mercado Pago) e histórico")
public class FakePaymentWebhookController {

    private final HandlePaymentNotificationUseCase handlePaymentNotification;

    @PostMapping("/webhook")
    @Operation(summary = "Webhook simulado (só ambiente local): aprova/recusa a cobrança fake")
    public ResponseEntity<Void> webhook(@Valid @RequestBody WebhookPayload payload) {
        handlePaymentNotification.execute(payload.gatewayPaymentId(), payload.approved());
        return ResponseEntity.ok().build();
    }
}
