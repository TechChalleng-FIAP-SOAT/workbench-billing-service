package com.workbench.workshop.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageEnvelope(
        String messageId, String sagaId, String serviceOrderId,
        String type, String occurredAt, Map<String, Object> payload
) {
    public static MessageEnvelope of(String type, String sagaId, String serviceOrderId, Map<String, Object> payload) {
        return new MessageEnvelope(UUID.randomUUID().toString(), sagaId, serviceOrderId, type,
                Instant.now().toString(), payload);
    }
}
