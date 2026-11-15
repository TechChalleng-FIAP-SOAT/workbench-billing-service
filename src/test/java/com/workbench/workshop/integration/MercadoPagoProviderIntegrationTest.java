package com.workbench.workshop.integration;

import com.workbench.workshop.infrastructure.scheduling.PaymentStatusPollingJob;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Com o Mercado Pago real ({@code payment.provider=mercadopago}, produção) o webhook
 * simulado não pode existir e o job de consulta deve subir (com ShedLock e a tabela
 * {@code shedlock} da migration). O polling fica desligado no teste (intervalo enorme)
 * para não chamar a API externa.
 */
@SpringBootTest(properties = {
        "payment.provider=mercadopago",
        "payment.mercadopago.access-token=TEST-token",
        "payment.polling-interval-ms=3600000"
})
@AutoConfigureMockMvc
@Testcontainers
class MercadoPagoProviderIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void requireDocker() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponível — pulando teste de integração.");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationContext context;

    @Test
    void simulatedWebhookIsNotExposed() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gatewayPaymentId\":\"PREF-1\",\"approved\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pollingJobIsActive() {
        assertNotNull(context.getBean(PaymentStatusPollingJob.class));
    }
}
