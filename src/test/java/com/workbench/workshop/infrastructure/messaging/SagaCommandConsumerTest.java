package com.workbench.workshop.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SagaCommandConsumerTest {

    private static final String QUEUE = "http://localhost:4566/q/billing-commands";

    private SqsClient sqsClient;
    private SagaCommandHandler handler;
    private SagaCommandConsumer consumer;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        sqsClient = mock(SqsClient.class);
        handler = mock(SagaCommandHandler.class);
        MessagingProperties props = new MessagingProperties();
        props.setEnabled(true);
        props.setCommandsQueueUrl(QUEUE);
        consumer = new SagaCommandConsumer(sqsClient, props, mapper, handler);
    }

    private Message msg(String id, String receipt) throws Exception {
        var env = new MessageEnvelope(id, "saga-1", UUID.randomUUID().toString(),
                "ComandoGerarOrcamento", "2026-01-01T00:00:00Z", Map.of("items", java.util.List.of()));
        return Message.builder().body(mapper.writeValueAsString(env)).receiptHandle(receipt).build();
    }

    @Test
    void pollProcessesAndDeletes() throws Exception {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(msg("m1", "r1")).build());
        consumer.poll();
        verify(handler).handle(any(MessageEnvelope.class));
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void deduplicates() throws Exception {
        Message m = msg("dup", "r1");
        consumer.handleMessage(QUEUE, m);
        consumer.handleMessage(QUEUE, m);
        verify(handler, times(1)).handle(any());
        verify(sqsClient, times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void leavesForDlqOnFailure() throws Exception {
        doThrow(new RuntimeException("boom")).when(handler).handle(any());
        consumer.handleMessage(QUEUE, msg("m2", "r2"));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollNoopWhenQueueMissing() {
        MessagingProperties empty = new MessagingProperties();
        empty.setEnabled(true);
        new SagaCommandConsumer(sqsClient, empty, mapper, handler).poll();
        verify(sqsClient, never()).receiveMessage(any(ReceiveMessageRequest.class));
    }
}
