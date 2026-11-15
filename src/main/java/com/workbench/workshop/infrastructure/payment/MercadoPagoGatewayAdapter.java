package com.workbench.workshop.infrastructure.payment;

import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway Mercado Pago ({@code payment.provider=mercadopago}) — Checkout Pro.
 *
 * <p>Cria uma preferência de pagamento ({@code POST /checkout/preferences}) com
 * {@code external_reference = id da OS}; o cliente paga pelo link ({@code init_point}).
 * A confirmação é obtida por consulta ({@code GET /v1/payments/search?external_reference=})
 * feita pelo job {@code PaymentStatusPollingJob} — sem webhook público. A troca
 * sandbox→produção é apenas o access-token (ADR-0005).
 */
@Component
@ConditionalOnProperty(prefix = "payment", name = "provider", havingValue = "mercadopago")
@Slf4j
public class MercadoPagoGatewayAdapter implements PaymentGatewayPort {

    private final RestClient restClient;
    private final String notificationUrl;

    public MercadoPagoGatewayAdapter(MercadoPagoProperties props) {
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getAccessToken())
                .build();
        this.notificationUrl = props.getNotificationUrl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public GatewayCharge createCharge(String chargeReference, BigDecimal amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(Map.of(
                "title", "Ordem de Serviço " + chargeReference,
                "quantity", 1,
                "unit_price", amount,
                "currency_id", "BRL")));
        body.put("external_reference", chargeReference);
        // Opcional: a confirmação vem do job de consulta; só envia se configurado.
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl);
        }

        Map<String, Object> response = restClient.post()
                .uri("/checkout/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        String id = response != null ? String.valueOf(response.get("id")) : null;
        String checkoutUrl = response != null ? String.valueOf(response.getOrDefault("init_point", "")) : "";
        log.info("[MercadoPago] preferência criada: ref={}, prefId={}", chargeReference, id);
        return new GatewayCharge(id, checkoutUrl);
    }

    @Override
    @SuppressWarnings("unchecked")
    public GatewayPaymentResult findPayment(String chargeReference) {
        Map<String, Object> response = restClient.get()
                .uri("/v1/payments/search?external_reference={ref}&sort=date_created&criteria=desc", chargeReference)
                .retrieve()
                .body(Map.class);
        List<Map<String, Object>> results = response == null ? List.of()
                : (List<Map<String, Object>>) response.getOrDefault("results", List.of());
        return toResult(results);
    }

    /** {@code POST /v1/payments/{id}/refunds} — estorno total; a chave de idempotência evita estorno duplo. */
    @Override
    public void refund(String transactionId) {
        restClient.post()
                .uri("/v1/payments/{id}/refunds", transactionId)
                .header("X-Idempotency-Key", "refund-" + transactionId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .toBodilessEntity();
        log.info("[MercadoPago] estorno solicitado: paymentId={}", transactionId);
    }

    /**
     * Converte as tentativas (mais recente primeiro) numa decisão. No Checkout Pro o
     * cliente pode errar o cartão ({@code rejected}) e tentar de novo no mesmo link,
     * então só {@code approved} em qualquer tentativa ou {@code cancelled} na mais
     * recente encerram a cobrança; o resto segue PENDING.
     */
    static GatewayPaymentResult toResult(List<Map<String, Object>> attemptsNewestFirst) {
        for (Map<String, Object> attempt : attemptsNewestFirst) {
            if ("approved".equals(attempt.get("status"))) {
                return new GatewayPaymentResult(GatewayPaymentStatus.APPROVED, String.valueOf(attempt.get("id")));
            }
        }
        if (!attemptsNewestFirst.isEmpty() && "cancelled".equals(attemptsNewestFirst.get(0).get("status"))) {
            return new GatewayPaymentResult(GatewayPaymentStatus.REJECTED, null);
        }
        return GatewayPaymentResult.pending();
    }
}
