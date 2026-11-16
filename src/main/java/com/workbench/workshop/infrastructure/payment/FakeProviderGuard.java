package com.workbench.workshop.infrastructure.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Impede que o gateway simulado suba fora de desenvolvimento e testes.
 *
 * <p>Com {@code payment.provider=fake} existe o webhook {@code POST /api/payments/webhook}, que
 * não verifica assinatura e aprova ou recusa qualquer cobrança. Se um ConfigMap ou variável de
 * ambiente errada ligasse o {@code fake} na AWS, qualquer chamada autenticada entregaria a OS
 * sem pagamento. Por isso o serviço só sobe com o {@code fake} no perfil {@code local} ou quando
 * {@code payment.allow-fake=true} (usado pelos testes automatizados). Caso contrário, falha na
 * inicialização.
 */
@Component
@ConditionalOnProperty(prefix = "payment", name = "provider", havingValue = "fake")
public class FakeProviderGuard {

    public FakeProviderGuard(Environment environment,
                             @Value("${payment.allow-fake:false}") boolean allowFake) {
        if (!allowFake && !environment.acceptsProfiles(Profiles.of("local"))) {
            throw new IllegalStateException(
                    "payment.provider=fake só é permitido no perfil 'local' (ou com payment.allow-fake=true "
                            + "em testes): ele expõe um webhook sem assinatura que aprova pagamentos. "
                            + "Use PAYMENT_PROVIDER=mercadopago.");
        }
    }
}
