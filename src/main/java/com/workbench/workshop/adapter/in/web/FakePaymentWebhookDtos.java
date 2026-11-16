package com.workbench.workshop.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/** DTO do webhook simulado (só ambiente local). */
public final class FakePaymentWebhookDtos {

    private FakePaymentWebhookDtos() {}

    public record WebhookPayload(@NotBlank String gatewayPaymentId, boolean approved) {}
}
