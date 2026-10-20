package com.workbench.workshop.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Long-polling da fila de comandos do Billing, com dedupe e redrive para DLQ. */
@Component
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true")
@Slf4j
public class SagaCommandConsumer {

    private static final int DEDUP_CAPACITY = 1000;
    private final Map<String, Boolean> processed = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > DEDUP_CAPACITY;
                }
            });

    private final SqsClient sqsClient;
    private final MessagingProperties props;
    private final ObjectMapper objectMapper;
    private final SagaCommandHandler handler;

    public SagaCommandConsumer(SqsClient sqsClient, MessagingProperties props,
                               ObjectMapper objectMapper, SagaCommandHandler handler) {
        this.sqsClient = sqsClient;
        this.props = props;
        this.objectMapper = objectMapper;
        this.handler = handler;
    }

    @Scheduled(fixedDelayString = "${messaging.poll-interval-ms:5000}")
    public void poll() {
        String queueUrl = props.getCommandsQueueUrl();
        if (queueUrl == null || queueUrl.isBlank()) {
            return;
        }
        var request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl).maxNumberOfMessages(10).waitTimeSeconds(5).build();
        for (Message message : sqsClient.receiveMessage(request).messages()) {
            handleMessage(queueUrl, message);
        }
    }

    void handleMessage(String queueUrl, Message message) {
        try {
            MessageEnvelope cmd = objectMapper.readValue(message.body(), MessageEnvelope.class);
            if (processed.containsKey(cmd.messageId())) {
                delete(queueUrl, message);
                return;
            }
            // Propaga a correlação da Saga para o MDC: com o encoder JSON, saga_id e
            // service_order_id viram campos pesquisáveis no Datadog, permitindo
            // rastrear uma OS ponta a ponta (@saga_id) através dos microsserviços.
            MDC.put("saga_id", cmd.sagaId());
            MDC.put("service_order_id", cmd.serviceOrderId());
            try {
                handler.handle(cmd);
                processed.put(cmd.messageId(), Boolean.TRUE);
                delete(queueUrl, message);
            } finally {
                MDC.remove("saga_id");
                MDC.remove("service_order_id");
            }
        } catch (Exception e) {
            log.error("Failed to process command; leaving for redrive/DLQ. body={}", message.body(), e);
        }
    }

    private void delete(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
    }
}
