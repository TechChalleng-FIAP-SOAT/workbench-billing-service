package com.workbench.workshop.integration;

import com.workbench.workshop.application.budget.GenerateBudgetUseCase;
import com.workbench.workshop.application.payment.CreatePaymentUseCase;
import com.workbench.workshop.application.payment.port.out.PaymentGatewayPort;
import com.workbench.workshop.domain.budget.ItemType;
import com.workbench.workshop.domain.payment.Payment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Duas réplicas recebem o mesmo ComandoCobrarPagamento ao mesmo tempo: só uma
 * preferência é criada no gateway, e as duas devolvem a mesma cobrança.
 */
@SpringBootTest
@Testcontainers
class ConcurrentChargeIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void requireDocker() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponível — pulando teste de integração.");
    }

    @Autowired private GenerateBudgetUseCase generateBudgetUseCase;
    @Autowired private CreatePaymentUseCase createPayment;
    @MockitoSpyBean private PaymentGatewayPort gateway;

    @Test
    void concurrentChargesCreateASinglePreference() throws Exception {
        UUID osId = UUID.randomUUID();
        generateBudgetUseCase.execute(osId, "52998224725", List.of(),
                List.of(new GenerateBudgetUseCase.LaborInput("ALINHAMENTO", null, new BigDecimal("80.00"))));

        // Gateway lento: sem a trava, as duas chamadas criariam preferência.
        doAnswer(invocation -> {
            Thread.sleep(500);
            return invocation.callRealMethod();
        }).when(gateway).createCharge(any(), any());

        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<Payment> first = CompletableFuture.supplyAsync(() -> charge(start, osId));
        CompletableFuture<Payment> second = CompletableFuture.supplyAsync(() -> charge(start, osId));
        start.countDown();

        assertEquals(first.get().getGatewayPaymentId(), second.get().getGatewayPaymentId());
        verify(gateway, times(1)).createCharge(any(), any());
    }

    private Payment charge(CountDownLatch start, UUID osId) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return createPayment.execute(osId);
    }
}
