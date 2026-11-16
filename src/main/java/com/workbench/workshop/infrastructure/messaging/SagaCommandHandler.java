package com.workbench.workshop.infrastructure.messaging;

import com.workbench.workshop.application.budget.GenerateBudgetUseCase;
import com.workbench.workshop.application.budget.GenerateBudgetUseCase.LaborInput;
import com.workbench.workshop.application.budget.GenerateBudgetUseCase.PartInput;
import com.workbench.workshop.application.payment.CreatePaymentUseCase;
import com.workbench.workshop.application.payment.RefundPaymentUseCase;
import com.workbench.workshop.domain.budget.Budget;
import com.workbench.workshop.domain.budget.ItemType;
import com.workbench.workshop.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.workbench.workshop.infrastructure.messaging.SagaMessages.*;

/**
 * Traduz comandos da Saga em casos de uso do Billing e publica os eventos de resposta.
 *
 * <ul>
 *   <li>{@code ComandoGerarOrcamento} → gera orçamento → {@code OrcamentoGerado}.</li>
 *   <li>{@code ComandoCobrarPagamento} → cria cobrança (PENDENTE) → {@code CobrancaCriada}
 *       (a confirmação virá depois, pelo job de consulta ao gateway).</li>
 *   <li>{@code ComandoEstornarPagamento} → estorna → {@code PagamentoEstornado}.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SagaCommandHandler {

    private final GenerateBudgetUseCase generateBudgetUseCase;
    private final CreatePaymentUseCase createPayment;
    private final RefundPaymentUseCase refundPayment;
    private final BillingEventPublisher publisher;

    public void handle(MessageEnvelope cmd) {
        UUID serviceOrderId = UUID.fromString(cmd.serviceOrderId());
        log.info("Comando recebido: type={}, serviceOrderId={}", cmd.type(), serviceOrderId);
        switch (cmd.type()) {
            case GENERATE_BUDGET -> generateBudget(cmd, serviceOrderId);
            case CHARGE_PAYMENT -> {
                Payment payment = createPayment.execute(serviceOrderId, cmd.sagaId());
                log.info("Cobrança criada: gatewayPaymentId={}, serviceOrderId={}",
                        payment.getGatewayPaymentId(), serviceOrderId);
                reply(cmd, CHARGE_CREATED, payloadOf("gatewayPaymentId", payment.getGatewayPaymentId(),
                        "checkoutUrl", payment.getCheckoutUrl()));
            }
            case REFUND_PAYMENT -> {
                Payment payment = refundPayment.execute(serviceOrderId);
                log.info("Pagamento estornado: status={}, serviceOrderId={}",
                        payment.getStatus().name(), serviceOrderId);
                reply(cmd, PAYMENT_REFUNDED, payloadOf("status", payment.getStatus().name()));
            }
            default -> log.warn("Comando desconhecido ignorado: type={}", cmd.type());
        }
    }

    private void generateBudget(MessageEnvelope cmd, UUID serviceOrderId) {
        try {
            // A leitura do payload fica dentro do try: um item malformado também é
            // OrcamentoFalhou (resposta imediata), não uma mensagem perdida na DLQ.
            Budget budget = generateBudgetUseCase.execute(serviceOrderId,
                    stringOrNull(cmd.payload().get("customerDocument")),
                    parts(cmd.payload()), labor(cmd.payload()));
            log.info("Orçamento gerado: budgetId={}, totalPrice={}, serviceOrderId={}",
                    budget.getId(), budget.getTotalPrice(), serviceOrderId);
            reply(cmd, BUDGET_GENERATED, payloadOf("budgetId", budget.getId(),
                    "totalParts", budget.getTotalParts(),
                    "totalLabor", budget.getTotalLabor(),
                    "totalPrice", budget.getTotalPrice()));
        } catch (RuntimeException e) {
            // Falha de negócio é um resultado da Saga: responde OrcamentoFalhou (não relança
            // — a decisão de retry/compensação cabe ao orquestrador no OS Service).
            log.warn("Falha ao gerar orçamento para OS {}: {}", serviceOrderId, e.getMessage());
            reply(cmd, BUDGET_FAILED, payloadOf("reason", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<PartInput> parts(Map<String, Object> payload) {
        var raw = (List<Map<String, Object>>) payload.getOrDefault("parts", List.of());
        return raw.stream()
                .map(i -> new PartInput(
                        stringOrNull(i.get("productId")),
                        required(i, "name", "peça"),
                        stringOrNull(i.get("sku")),
                        stringOrNull(i.get("brand")),
                        ItemType.valueOf(required(i, "itemType", "peça")),
                        new BigDecimal(required(i, "unitPrice", "peça")),
                        new BigDecimal(required(i, "quantity", "peça"))))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<LaborInput> labor(Map<String, Object> payload) {
        var raw = (List<Map<String, Object>>) payload.getOrDefault("labor", List.of());
        return raw.stream()
                .map(i -> new LaborInput(
                        required(i, "serviceName", "mão de obra"),
                        stringOrNull(i.get("laborCode")),
                        new BigDecimal(required(i, "laborCost", "mão de obra"))))
                .toList();
    }

    private void reply(MessageEnvelope cmd, String type, Map<String, Object> payload) {
        publisher.publish(MessageEnvelope.of(type, cmd.sagaId(), cmd.serviceOrderId(), payload));
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Campo obrigatório do item: ausente vira erro (e o orçamento responde OrcamentoFalhou),
     * em vez de entrar no orçamento como o texto "null".
     */
    private static String required(Map<String, Object> item, String field, String itemKind) {
        Object value = item.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório ausente no item de " + itemKind + ": " + field);
        }
        return String.valueOf(value);
    }

    /** Monta um payload tolerando valores nulos (diferente de Map.of). */
    private static Map<String, Object> payloadOf(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}
