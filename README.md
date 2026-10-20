# workbench-billing-service — Billing Service (Tech Challenge Fase 4)

Microsserviço de **orçamento e pagamento** da plataforma Workbench. Gera o
orçamento da OS (snapshot de preço) e processa o pagamento via **Mercado Pago**,
com histórico append-only. Arquitetura Hexagonal, **PostgreSQL** + Flyway.

## Responsabilidades

- **Orçamento**: montado a partir dos itens recebidos no comando `ComandoGerarOrcamento`
  (já precificados pelo Execution); congela o snapshot de preço.
- **Pagamento**: ocorre **no fim do fluxo** (ADR-0005). Máquina de estados
  `PENDENTE → APROVADO/RECUSADO`, e `APROVADO → ESTORNADO` (compensação da Saga).
- **Histórico de pagamento** (append-only) para auditoria.

## Integração Mercado Pago via porta

`PaymentGatewayPort` com adapters intercambiáveis (`payment.provider`):

| provider | adapter | uso |
|---|---|---|
| `fake` (default) | `FakePaymentGatewayAdapter` | testes/dev e demonstração da Saga, offline |
| `mercadopago` | `MercadoPagoGatewayAdapter` | sandbox (homolog) / produção |

Trocar sandbox→produção é apenas `MP_ACCESS_TOKEN`/`MP_BASE_URL` — **sem mudança de
código** (ADR-0005). A confirmação chega via webhook em `POST /api/payments/webhook`.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/budgets/service-order/{osId}` | Orçamento da OS |
| POST | `/api/payments/service-order/{osId}` | Criar cobrança (PENDENTE) |
| POST | `/api/payments/webhook` | Notificação do gateway (aprova/recusa) |
| GET | `/api/payments/service-order/{osId}/history` | Histórico de pagamento |

## Saga (mensageria)

| Recebe (comando) | Faz | Publica (evento) |
|---|---|---|
| `ComandoGerarOrcamento` | gera orçamento (snapshot) | `OrcamentoGerado` |
| `ComandoCobrarPagamento` | cria cobrança no gateway | `CobrancaCriada` (confirma via webhook) |
| `ComandoEstornarPagamento` | estorna pagamento | `PagamentoEstornado` |

Consumer com dedupe por `messageId` e redrive para DLQ.

## Qualidade

- Cobertura **86.4%** (JaCoCo, gate 80%); unit + integração com Testcontainers Postgres.
- Pipeline: `unit-test → code-analysis (JaCoCo+Sonar) → build → docker → deploy EKS`.

## Status na Fase 4

- [x] Orçamento a partir de itens recebidos (snapshot)
- [x] Pagamento (máquina de estados) + histórico append-only
- [x] `PaymentGatewayPort` (fake + Mercado Pago sandbox) + webhook
- [x] Consumer/publisher da Saga
- [ ] Orquestração ponta a ponta (Etapa 5)
