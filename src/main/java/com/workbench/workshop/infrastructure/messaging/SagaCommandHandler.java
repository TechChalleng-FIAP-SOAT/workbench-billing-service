package com.workbench.workshop.infrastructure.messaging;

import com.workbench.workshop.application.budget.GenerateBudgetUseCase;
import com.workbench.workshop.application.payment.PaymentUseCases;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.budget.ItemType;
import com.workbench.workshop.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Traduz comandos da Saga em casos de uso do Billing e publica os eventos de resposta.
 *
 * <ul>
 *   <li>{@code ComandoGerarOrcamento} → gera orçamento → {@code OrcamentoGerado}.</li>
 *   <li>{@code ComandoCobrarPagamento} → cria cobrança (PENDENTE) → {@code CobrancaCriada}
 *       (a confirmação virá do webhook do gateway).</li>
 *   <li>{@code ComandoEstornarPagamento} → estorna → {@code PagamentoEstornado}.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SagaCommandHandler {

    private final GenerateBudgetUseCase generateBudgetUseCase;
    private final PaymentUseCases paymentUseCases;
    private final BillingEventPublisher publisher;

    @SuppressWarnings("unchecked")
    public void handle(MessageEnvelope cmd) {
        UUID serviceOrderId = UUID.fromString(cmd.serviceOrderId());
        switch (cmd.type()) {
            case "ComandoGerarOrcamento" -> {
                List<Map<String, Object>> rawParts =
                        (List<Map<String, Object>>) cmd.payload().getOrDefault("parts", List.of());
                List<GenerateBudgetUseCase.PartInput> parts = rawParts.stream()
                        .map(i -> new GenerateBudgetUseCase.PartInput(
                                i.get("productId") == null ? null : String.valueOf(i.get("productId")),
                                String.valueOf(i.get("name")),
                                String.valueOf(i.get("sku")),
                                i.get("brand") == null ? null : String.valueOf(i.get("brand")),
                                ItemType.valueOf(String.valueOf(i.get("itemType"))),
                                new BigDecimal(String.valueOf(i.get("unitPrice"))),
                                new BigDecimal(String.valueOf(i.get("quantity")))))
                        .toList();

                List<Map<String, Object>> rawLabor =
                        (List<Map<String, Object>>) cmd.payload().getOrDefault("labor", List.of());
                List<GenerateBudgetUseCase.LaborInput> labor = rawLabor.stream()
                        .map(i -> new GenerateBudgetUseCase.LaborInput(
                                String.valueOf(i.get("serviceName")),
                                i.get("laborCode") == null ? null : String.valueOf(i.get("laborCode")),
                                new BigDecimal(String.valueOf(i.get("laborCost")))))
                        .toList();

                try {
                    Budget budget = generateBudgetUseCase.execute(serviceOrderId, parts, labor);
                    publisher.publish(MessageEnvelope.of("OrcamentoGerado", cmd.sagaId(), cmd.serviceOrderId(),
                            payloadOf("budgetId", budget.getId(),
                                    "totalParts", budget.getTotalParts(),
                                    "totalLabor", budget.getTotalLabor(),
                                    "totalPrice", budget.getTotalPrice())));
                } catch (RuntimeException e) {
                    // Falha de negócio na geração do orçamento é um resultado da Saga:
                    // responde OrcamentoFalhou (não relança — a mensagem é consumida; a
                    // decisão de retry/compensação cabe ao orquestrador no OS Service).
                    log.warn("Falha ao gerar orçamento para OS {}: {}", serviceOrderId, e.getMessage());
                    publisher.publish(MessageEnvelope.of("OrcamentoFalhou", cmd.sagaId(), cmd.serviceOrderId(),
                            payloadOf("reason", e.getMessage())));
                }
            }
            case "ComandoCobrarPagamento" -> {
                Payment payment = paymentUseCases.createPayment(serviceOrderId);
                publisher.publish(MessageEnvelope.of("CobrancaCriada", cmd.sagaId(), cmd.serviceOrderId(),
                        payloadOf("gatewayPaymentId", payment.getGatewayPaymentId())));
            }
            case "ComandoEstornarPagamento" -> {
                Payment payment = paymentUseCases.refund(serviceOrderId);
                publisher.publish(MessageEnvelope.of("PagamentoEstornado", cmd.sagaId(), cmd.serviceOrderId(),
                        payloadOf("status", payment.getStatus().name())));
            }
            default -> log.warn("Comando desconhecido ignorado: type={}", cmd.type());
        }
    }

    /** Monta um payload tolerando valores nulos (diferente de Map.of). */
    private static Map<String, Object> payloadOf(Object... kv) {
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}
