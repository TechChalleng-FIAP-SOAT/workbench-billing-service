package com.workbench.workshop.infrastructure.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "payment.mercadopago")
@Getter
@Setter
public class MercadoPagoProperties {
    private String accessToken;
    private String baseUrl = "https://api.mercadopago.com";
    private String notificationUrl;
    /** Tempo máximo para abrir a conexão com o Mercado Pago. */
    private Duration connectTimeout = Duration.ofSeconds(5);
    /**
     * Tempo máximo esperando a resposta. Sem ele, um Mercado Pago que aceita a conexão e
     * não responde prende o job de consulta (e nenhum pagamento é confirmado).
     */
    private Duration readTimeout = Duration.ofSeconds(10);
}
