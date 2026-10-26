# workbench-billing-service — Billing Service

Microsserviço de **orçamento e pagamento** da plataforma Workbench (Tech Challenge —
FIAP Pós Tech, Fase 4). Gera o orçamento da OS e processa o pagamento via
**Mercado Pago**, com histórico append-only.

> **Contexto (Fase 4):** na Fase 3 a oficina era um **monólito** (`workbench-monolith`,
> mantido como base histórica). Na Fase 4 a aplicação foi refatorada em
> **microsserviços independentes**, cada um com seu repositório, infraestrutura e
> banco próprios. Este repositório é o **Billing Service**. A divisão dos serviços e a
> estratégia da Saga estão nos ADRs (`workbench-monolith/docs/adr` — ADR-0003 e ADR-0005).

---

## Sumário

- [Responsabilidades](#responsabilidades)
- [Requisitos obrigatórios do PDF cobertos aqui](#requisitos-obrigatórios-do-pdf-cobertos-aqui)
- [Integração Mercado Pago (via porta)](#integração-mercado-pago-via-porta)
- [Participação na Saga](#participação-na-saga)
- [Arquitetura da aplicação (hexagonal)](#arquitetura-da-aplicação-hexagonal)
- [Arquitetura de infraestrutura](#arquitetura-de-infraestrutura)
- [Fluxo de Deploy (CI/CD)](#fluxo-de-deploy-cicd)
- [Tecnologias](#tecnologias)
- [Como executar localmente](#como-executar-localmente)
- [Deploy em Kubernetes / AWS](#deploy-em-kubernetes--aws)
- [Referência de endpoints](#referência-de-endpoints)
- [Testes e cobertura](#testes-e-cobertura)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)

---

## Responsabilidades

- **Orçamento**: montado a partir dos itens recebidos no `ComandoGerarOrcamento` —
  **peças** (precificadas pelo Execution, com marca) + **mão de obra** por serviço
  (item `SERVICE_COST`, vinda do OS Service). Congela um snapshot de preço; expõe
  total de peças, total de MO e total geral.
- **Pagamento**: ocorre **no fim do fluxo** (ADR-0005). Máquina de estados
  `PENDENTE → APROVADO/RECUSADO` e `APROVADO → ESTORNADO`.
- **Histórico de pagamento** (append-only) para auditoria e verificação.

---

## Requisitos obrigatórios do PDF cobertos aqui

| Requisito (PDF Fase 4) | Como este serviço atende |
|---|---|
| ≥3 microsserviços independentes | É um dos 4 serviços, com repo/infra/banco próprios |
| Banco próprio | **PostgreSQL** exclusivo; nenhum outro serviço o acessa |
| SQL **e** NoSQL (no conjunto) | Contribui com o **SQL** (PostgreSQL) |
| Nenhum acesso ao banco de outro serviço | Recebe os itens já precificados por mensagem; **não** acessa estoque/OS |
| REST + mensageria assíncrona | API REST (orçamento/pagamento/webhook) + **SNS/SQS** (comandos/eventos) |
| **Pagamento Mercado Pago** | `PaymentGatewayPort` com adapter Mercado Pago (sandbox/produção) + webhook |
| Saga Pattern + compensação | Participa da Saga; publica `OrcamentoFalhou` para retry/compensação; estorno via `ComandoEstornarPagamento` |
| Testes + 80% cobertura + Sonar | JaCoCo gate 80% no `verify` + SonarCloud (cobertura ~88%) |
| CI/CD + deploy K8s + branch protegida | Pipeline próprio; `main` protegida |

---

## Integração Mercado Pago (via porta)

`PaymentGatewayPort` com adapters intercambiáveis (`payment.provider`):

| provider | adapter | uso |
|---|---|---|
| `fake` (default) | `FakePaymentGatewayAdapter` | testes/dev e demonstração da Saga, offline |
| `mercadopago` | `MercadoPagoGatewayAdapter` | sandbox (homologação) / produção |

Trocar sandbox→produção é apenas `MP_ACCESS_TOKEN`/`MP_BASE_URL` — **sem mudança de
código**. A confirmação chega via **webhook** em `POST /api/payments/webhook`.

---

## Participação na Saga

| Recebe (comando) | Faz | Publica (evento) |
|---|---|---|
| `ComandoGerarOrcamento` | gera orçamento (peças + MO) | `OrcamentoGerado` ou `OrcamentoFalhou` (motivo) |
| `ComandoCobrarPagamento` | cria cobrança no gateway | `CobrancaCriada` (confirma via webhook) |
| `ComandoEstornarPagamento` | estorna pagamento | `PagamentoEstornado` |

`OrcamentoFalhou` permite ao orquestrador (OS Service) fazer **retry** e, esgotado,
**compensar** liberando a reserva de estoque (ver ADR-0005). Consumer com dedupe por
`messageId` e redrive para **DLQ**.

---

## Arquitetura da aplicação (hexagonal)

```
src/main/java/com/workbench/workshop/
├── adapter/
│   ├── in/web/            # BudgetController, PaymentController (+ webhook)
│   └── out/persistence/   # Adapters JPA (budget, payment, histórico)
├── application/           # Use cases + ports (GenerateBudget, PaymentUseCases)
├── domain/                # Budget, BudgetItem (PART/SUPPLY/SERVICE_COST), Payment (máquina de estados)
└── infrastructure/        # config, persistence (JPA), messaging (SNS/SQS), payment (fake/mercadopago)
```

---

## Arquitetura de infraestrutura

Banco **PostgreSQL próprio**, namespace `billing-service` no cluster EKS compartilhado
`workbench-platform-<env>` (provisionado pelo `workbench-k8s-terraform`). Sem acesso
cruzado a bancos; integra com os demais por REST/mensageria. Tópicos/filas no repo
`workbench-messaging`.

---

## Fluxo de Deploy (CI/CD)

```
push (main/develop) ou PR
   │
   ▼
unit-test ──▶ code-analysis ──▶ build ──▶ docker-build-push ──▶ deploy-aws
(mvn verify   (JaCoCo gate 80%  (mvn      (imagem p/ Docker Hub  (EKS, só se
 + JaCoCo      + Sonar)          package)  usuario/billing-service) USE_AWS=true)
 badge)
```

Secrets/variáveis idênticos aos demais serviços (`DOCKER_*`, `SONAR_TOKEN`, `AWS_*`,
`USE_AWS`) — ver o README do `workbench-os-service` para a tabela completa. Pagamento
real exige `MP_ACCESS_TOKEN` (sandbox/produção).

---

## Tecnologias

| Tecnologia | Finalidade |
|---|---|
| Java 21 + Spring Boot 4.0.5 | Framework web e DI |
| PostgreSQL 16 + Flyway | Banco próprio (SQL) + migrations |
| AWS SNS/SQS (SDK v2) | Mensageria da Saga + DLQ |
| Mercado Pago (REST) | Gateway de pagamento (via porta) |
| Micrometer + Datadog | Métricas e observabilidade |
| SpringDoc OpenAPI | Swagger UI |
| JaCoCo (gate 80%) + SonarCloud | Qualidade e cobertura |
| Testcontainers | Integração com PostgreSQL real |

---

## Como executar localmente

```bash
./mvnw spring-boot:run
```

| Variável | Default | Descrição |
|---|---|---|
| `PAYMENT_PROVIDER` | `fake` | `fake` (offline) ou `mercadopago` (sandbox/produção) |
| `MP_ACCESS_TOKEN` / `MP_BASE_URL` | — | credenciais do Mercado Pago (quando `mercadopago`) |
| `MESSAGING_ENABLED` | `false` | liga SNS/SQS; `false` roda sem broker |
| `AWS_ENDPOINT` | — | vazio = AWS real; `http://localhost:4566` = LocalStack |

---

## Deploy em Kubernetes / AWS

Manifests em `k8s/` (`namespace`, `configmap`, `secret`, `app`, `hpa`). Deploy
automatizado pelo pipeline (`deploy-aws`, quando `USE_AWS=true`) no cluster
`workbench-platform-<branch>`, namespace `billing-service`.

---

## Segurança (JWT)

Defesa em profundidade: o serviço **valida o JWT por conta própria** (Spring Security +
filtro HS256 com o `JWT_SECRET` compartilhado), inclusive nas chamadas internas — não
depende do API Gateway. Tokens de staff carregam `roles` (ADMIN/TECHNICIAN); tokens de
cliente carregam `clientId` → `ROLE_CLIENT`.

Rotas **públicas** (sem token):
- `GET /actuator/health*`, Swagger/OpenAPI;
- `POST /api/payments/webhook` — chamado pelo **gateway de pagamento externo** (Mercado
  Pago), que não envia o nosso JWT.

As consultas (`GET /api/budgets/**`, `GET /api/payments/**`) são liberadas ao cliente
dono (`ROLE_CLIENT`) e ao staff; as demais rotas exigem ADMIN/TECHNICIAN.

### Boas práticas OWASP aplicadas
- **A01/A07 (acesso):** autorização por role em todas as rotas; JWT validado por serviço.
- **A03 (injeção/validação):** `@Valid` + Bean Validation nos DTOs de entrada; acesso a
  dados via Spring Data JPA parametrizado (sem concatenação de SQL).
- **A05 (misconfiguration):** headers `X-Frame-Options: DENY` e `X-Content-Type-Options:
  nosniff`; `k8s/secret.yaml` com **placeholders** (`REPLACE_ME`) — segredos reais vêm do
  secret manager no deploy, nunca versionados.
- **A09 (logs):** o CPF do cliente é mascarado no log (MDC) — nunca registrado por inteiro.

> Hotspot SonarCloud `java:S4502` (CSRF disabled) é marcado **Safe**: API REST stateless
> com JWT, sem cookie de sessão.

---

## Referência de endpoints

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/api/budgets/service-order/{osId}` | Orçamento da OS (peças, MO e totais) | token |
| GET | `/api/payments/service-order/{osId}` | Pagamento da OS (status + **checkoutUrl**) | token |
| POST | `/api/payments/service-order/{osId}` | Criar cobrança (PENDENTE) — normalmente **automática** via Saga | staff |
| POST | `/api/payments/webhook` | Notificação do gateway (aprova/recusa) | público |
| GET | `/api/payments/service-order/{osId}/history` | Histórico de pagamento | token |

> **Link de pagamento (checkout):** ao criar a cobrança, o gateway devolve um
> `checkoutUrl`. Ele é **persistido** e enviado ao cliente por e-mail (via os-service, no
> evento `CobrancaCriada`). Caso o cliente perca o e-mail, consulta o link em
> `GET /api/payments/service-order/{osId}` (campo `checkoutUrl`).

---

## Testes e cobertura

```bash
./mvnw verify
```

- **Gate JaCoCo 80%** no `verify`.
- Integração com **Testcontainers PostgreSQL**; gateway em modo `fake` (offline).
- Cobertura atual: **~88%** (`target/site/jacoco/index.html`).

---

## Documentação da API (Swagger)

| Recurso | URL (local) |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
