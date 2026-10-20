package com.workbench.workshop.infrastructure.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment.mercadopago")
@Getter
@Setter
public class MercadoPagoProperties {
    private String accessToken;
    private String baseUrl = "https://api.mercadopago.com";
    private String notificationUrl;
}
