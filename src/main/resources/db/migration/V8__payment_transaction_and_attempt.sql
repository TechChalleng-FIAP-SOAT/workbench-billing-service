-- =====================================================================
-- V8 — Estorno real e nova cobrança após recusa.
--  * gateway_transaction_id: id do pagamento APROVADO no Mercado Pago (o
--    gateway_payment_id é o da preferência/checkout). O estorno precisa dele.
--  * attempt: nº da cobrança da OS. Uma nova cobrança após recusa usa a
--    referência externa "<OS>-<attempt>", separada da tentativa recusada.
-- =====================================================================

ALTER TABLE payments ADD COLUMN gateway_transaction_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN attempt INT NOT NULL DEFAULT 1;
