-- =====================================================================
-- Billing Service — schema inicial (Fase 4, Etapa 4)
-- Orçamento (budget + itens) e pagamento (payment + histórico append-only).
-- =====================================================================

CREATE SEQUENCE budget_seq        START 1 INCREMENT 50;
CREATE SEQUENCE budget_item_seq   START 1 INCREMENT 50;
CREATE SEQUENCE payment_seq       START 1 INCREMENT 50;
CREATE SEQUENCE payment_history_seq START 1 INCREMENT 50;

CREATE TABLE budgets (
    id               BIGINT PRIMARY KEY,
    service_order_id UUID NOT NULL UNIQUE,
    total_parts      NUMERIC(15,2) NOT NULL DEFAULT 0,   -- Σ peças/insumos (parte a)
    total_labor      NUMERIC(15,2) NOT NULL DEFAULT 0,   -- Σ mão de obra (parte b)
    total_price      NUMERIC(15,2) NOT NULL DEFAULT 0,   -- a + b
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

-- Item do orçamento: peça/insumo (PART/SUPPLY) ou mão de obra (SERVICE_COST).
-- product_id e sku são nulos para itens de mão de obra.
CREATE TABLE budget_items (
    id            BIGINT PRIMARY KEY,
    budget_id     BIGINT NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    product_id    BIGINT,
    name          VARCHAR(255) NOT NULL,
    sku           VARCHAR(255),
    item_type     VARCHAR(20)  NOT NULL CHECK (item_type IN ('PART','SUPPLY','SERVICE_COST')),
    quantity      NUMERIC(15,3) NOT NULL,
    unit_price    NUMERIC(15,2) NOT NULL,
    total_price   NUMERIC(15,2) NOT NULL
);
CREATE INDEX idx_budget_items_budget_id ON budget_items (budget_id);

CREATE TABLE payments (
    id                 BIGINT PRIMARY KEY,
    service_order_id   UUID NOT NULL UNIQUE,
    budget_id          BIGINT,
    amount             NUMERIC(15,2) NOT NULL,
    status             VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE','APROVADO','RECUSADO','ESTORNADO')),
    gateway_payment_id VARCHAR(255) UNIQUE,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP NOT NULL
);
CREATE INDEX idx_payments_service_order ON payments (service_order_id);

CREATE TABLE payment_history (
    id               BIGINT PRIMARY KEY,
    payment_id       BIGINT NOT NULL,
    service_order_id UUID NOT NULL,
    status           VARCHAR(20) NOT NULL,
    detail           VARCHAR(1000),
    created_at       TIMESTAMP NOT NULL
);
CREATE INDEX idx_payment_history_service_order ON payment_history (service_order_id);
