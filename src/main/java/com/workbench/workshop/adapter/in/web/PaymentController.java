package com.workbench.workshop.adapter.in.web;

import com.workbench.workshop.adapter.in.web.PaymentDtos.HistoryResponse;
import com.workbench.workshop.adapter.in.web.PaymentDtos.PaymentResponse;
import com.workbench.workshop.application.payment.CreatePaymentUseCase;
import com.workbench.workshop.application.payment.FindPaymentByServiceOrderUseCase;
import com.workbench.workshop.application.payment.FindPaymentHistoryUseCase;
import com.workbench.workshop.domain.payment.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Pagamento (Mercado Pago) e histórico")
public class PaymentController {

    private final CreatePaymentUseCase createPayment;
    private final FindPaymentByServiceOrderUseCase findPaymentByServiceOrder;
    private final FindPaymentHistoryUseCase findPaymentHistory;

    @PostMapping("/service-order/{serviceOrderId}")
    @Operation(summary = "Criar cobrança para a OS (pagamento pendente)")
    public ResponseEntity<PaymentResponse> create(@PathVariable UUID serviceOrderId) {
        Payment p = createPayment.execute(serviceOrderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(p));
    }

    @GetMapping("/service-order/{serviceOrderId}")
    @Operation(summary = "Buscar pagamento da OS")
    public ResponseEntity<PaymentResponse> findByServiceOrder(@PathVariable UUID serviceOrderId,
                                                              Authentication authentication) {
        return findPaymentByServiceOrder.execute(serviceOrderId, RequesterResolver.from(authentication))
                .map(p -> ResponseEntity.ok(PaymentResponse.from(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/service-order/{serviceOrderId}/history")
    @Operation(summary = "Histórico de pagamento da OS")
    public ResponseEntity<List<HistoryResponse>> history(@PathVariable UUID serviceOrderId,
                                                         Authentication authentication) {
        return ResponseEntity.ok(findPaymentHistory.execute(serviceOrderId, RequesterResolver.from(authentication)).stream()
                .map(HistoryResponse::from).toList());
    }
}
