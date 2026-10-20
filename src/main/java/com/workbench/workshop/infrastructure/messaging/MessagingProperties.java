package com.workbench.workshop.infrastructure.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "messaging")
@Getter
@Setter
public class MessagingProperties {
    private boolean enabled;
    private String region = "us-east-1";
    private String endpoint;
    private String billingEventsTopicArn;
    private String commandsQueueUrl;
    private long pollIntervalMs = 5000;
}
