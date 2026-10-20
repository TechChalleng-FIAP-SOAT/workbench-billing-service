package com.workbench.workshop.infrastructure.payment;

import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Gateway Mercado Pago ({@code payment.provider=mercadopago}).
 *
 * <p>Cria uma preferência de pagamento (Checkout) via API REST. A troca
 * sandbox→produção é apenas o access-token/base-url (ADR-0005) — nenhuma mudança
 * de código. O resultado final chega via webhook em {@code /api/payments/webhook}.
 *
 * <p>Implementação mínima da integração (criação da preferência). O parsing
 * detalhado de resposta pode ser estendido conforme a necessidade de demonstração.
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
    public GatewayCharge createCharge(UUID serviceOrderId, BigDecimal amount) {
        var body = Map.of(
                "items", java.util.List.of(Map.of(
                        "title", "Ordem de Serviço " + serviceOrderId,
                        "quantity", 1,
                        "unit_price", amount,
                        "currency_id", "BRL")),
                "external_reference", serviceOrderId.toString(),
                "notification_url", notificationUrl == null ? "" : notificationUrl);

        Map<String, Object> response = restClient.post()
                .uri("/checkout/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        String id = response != null ? String.valueOf(response.get("id")) : null;
        String checkoutUrl = response != null ? String.valueOf(response.getOrDefault("init_point", "")) : "";
        log.info("[MercadoPago] preferência criada: os={}, prefId={}", serviceOrderId, id);
        return new GatewayCharge(id, checkoutUrl);
    }
}
