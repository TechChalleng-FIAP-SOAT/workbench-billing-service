package com.workbench.workshop.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;
import java.util.function.Consumer;

@Configuration
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true")
@EnableScheduling
public class MessagingConfig {

    /**
     * {@link ObjectMapper} usado pelos publishers/consumers da Saga. No Spring
     * Boot 4 o {@code ObjectMapper} do Jackson 2 não é mais exposto como bean
     * por autoconfiguração, então o definimos explicitamente aqui.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper messagingObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public SnsClient snsClient(MessagingProperties props) {
        var builder = SnsClient.builder().region(Region.of(props.getRegion()));
        applyEndpoint(props, builder::endpointOverride, builder::credentialsProvider);
        return builder.build();
    }

    @Bean
    public SqsClient sqsClient(MessagingProperties props) {
        var builder = SqsClient.builder().region(Region.of(props.getRegion()));
        applyEndpoint(props, builder::endpointOverride, builder::credentialsProvider);
        return builder.build();
    }

    private void applyEndpoint(MessagingProperties props, Consumer<URI> endpointSetter,
                               Consumer<AwsCredentialsProvider> credsSetter) {
        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            endpointSetter.accept(URI.create(props.getEndpoint()));
            credsSetter.accept(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        } else {
            credsSetter.accept(DefaultCredentialsProvider.create());
        }
    }
}
