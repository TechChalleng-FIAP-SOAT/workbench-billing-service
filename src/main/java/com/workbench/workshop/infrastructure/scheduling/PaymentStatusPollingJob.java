package com.workbench.workshop.infrastructure.scheduling;

import com.workbench.workshop.application.payment.ListPendingPaymentsUseCase;
import com.workbench.workshop.application.payment.ReconcilePaymentUseCase;
import com.workbench.workshop.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Confirma os pagamentos consultando o Mercado Pago (em vez de um webhook público):
 * a cada janela, para cada pagamento PENDENTE, busca a situação no gateway pela
 * referência externa (id da OS) e aplica aprovado/recusado — o que publica
 * PagamentoConfirmado/PagamentoFalhou para a Saga. ShedLock garante uma única
 * execução por janela entre réplicas (HPA), evitando evento duplicado.
 */
@Component
@ConditionalOnProperty(prefix = "payment", name = "provider", havingValue = "mercadopago")
@RequiredArgsConstructor
@Slf4j
public class PaymentStatusPollingJob {

    private final ListPendingPaymentsUseCase listPendingPayments;
    private final ReconcilePaymentUseCase reconcilePayment;

    @Scheduled(fixedDelayString = "${payment.polling-interval-ms:30000}")
    @SchedulerLock(name = "payment-status-polling", lockAtMostFor = "PT2M", lockAtLeastFor = "PT5S")
    public void pollPendingPayments() {
        for (Payment payment : listPendingPayments.execute()) {
            try {
                var before = payment.getStatus(); // reconcile altera o próprio objeto
                Payment result = reconcilePayment.execute(payment);
                if (result.getStatus() != before) {
                    log.info("Pagamento conciliado com o gateway: os={}, status={}",
                            payment.getServiceOrderId(), result.getStatus());
                }
            } catch (RuntimeException e) {
                // Falha de um pagamento (ex.: gateway fora) não impede os demais; tenta na próxima janela.
                log.warn("Falha ao consultar pagamento no gateway: os={}: {}",
                        payment.getServiceOrderId(), e.getMessage());
            }
        }
    }
}
