package com.workbench.workshop.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true")
@Slf4j
public class BillingEventPublisher {

    private final SnsClient snsClient;
    private final MessagingProperties props;
    private final ObjectMapper objectMapper;

    public BillingEventPublisher(SnsClient snsClient, MessagingProperties props, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public void publish(MessageEnvelope envelope) {
        try {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(props.getBillingEventsTopicArn())
                    .message(objectMapper.writeValueAsString(envelope))
                    .build());
            log.info("Published {} to billing-events: sagaId={}", envelope.type(), envelope.sagaId());
        } catch (Exception e) {
            log.error("Failed to publish {} for sagaId={}", envelope.type(), envelope.sagaId(), e);
            throw new IllegalStateException("Failed to publish billing event", e);
        }
    }
}
