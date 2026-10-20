package com.workbench.workshop.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BillingEventPublisherTest {

    private SnsClient snsClient;
    private BillingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        snsClient = mock(SnsClient.class);
        MessagingProperties props = new MessagingProperties();
        props.setBillingEventsTopicArn("arn:aws:sns:us-east-1:000000000000:workbench-billing-events");
        publisher = new BillingEventPublisher(snsClient, props, new ObjectMapper());
    }

    @Test
    void publishesToTopic() {
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().messageId("m1").build());
        publisher.publish(MessageEnvelope.of("OrcamentoGerado", "saga-1", "os-1", Map.of("budgetId", 1)));
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        assertTrue(captor.getValue().topicArn().endsWith("workbench-billing-events"));
    }

    @Test
    void throwsWhenSnsFails() {
        when(snsClient.publish(any(PublishRequest.class))).thenThrow(new RuntimeException("down"));
        assertThrows(IllegalStateException.class,
                () -> publisher.publish(MessageEnvelope.of("X", "s", "o", Map.of())));
    }
}
