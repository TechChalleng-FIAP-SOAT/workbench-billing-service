package com.workbench.workshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workbench.workshop.application.budget.GenerateBudgetUseCase;
import com.workbench.workshop.domain.budget.ItemType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integração end-to-end do Billing com PostgreSQL real (Testcontainers):
 * gera orçamento (via use case), cria cobrança (fake gateway) e confirma via
 * webhook, validando o histórico de pagamento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BillingIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void requireDocker() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponível — pulando teste de integração.");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private GenerateBudgetUseCase generateBudgetUseCase;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void budgetToPaymentFlow() throws Exception {
        UUID osId = UUID.randomUUID();

        // 1. gera orçamento (como se viesse do ComandoGerarOrcamento): peças + MO
        generateBudgetUseCase.execute(osId,
                List.of(new GenerateBudgetUseCase.PartInput("1", "Pastilha", "PF-1", "Marca X", ItemType.PART,
                        new BigDecimal("50.00"), new BigDecimal("2"))),   // 100
                List.of(new GenerateBudgetUseCase.LaborInput("DISCO E PASTILHA", "00",
                        new BigDecimal("200.00"))));                       // 200 → total 300

        // 2. orçamento acessível via REST: peças 100 + MO 200 = 300
        mockMvc.perform(get("/api/budgets/service-order/" + osId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParts").value(100.00))
                .andExpect(jsonPath("$.totalLabor").value(200.00))
                .andExpect(jsonPath("$.totalPrice").value(300.00));

        // 3. cria cobrança (fake gateway) → PENDENTE
        MvcResult created = mockMvc.perform(post("/api/payments/service-order/" + osId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn();
        String gatewayId = mapper.readTree(created.getResponse().getContentAsString())
                .get("gatewayPaymentId").asText();

        // 4. webhook aprova
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gatewayPaymentId\":\"" + gatewayId + "\",\"approved\":true}"))
                .andExpect(status().isOk());

        // 5. histórico registra PENDENTE e APROVADO
        mockMvc.perform(get("/api/payments/service-order/" + osId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$[1].status").value("APROVADO"));
    }

    @Test
    void budgetNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/budgets/service-order/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPaymentWithoutBudgetReturns404() throws Exception {
        mockMvc.perform(post("/api/payments/service-order/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
