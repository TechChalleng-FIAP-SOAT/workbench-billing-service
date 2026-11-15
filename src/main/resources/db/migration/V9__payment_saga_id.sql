-- =====================================================================
-- V9 — Rodada da Saga a que a cobrança pertence.
--
-- O os-service só aceita resposta com o sagaId da rodada atual (ADR-0008). O resultado
-- do pagamento (PagamentoConfirmado/PagamentoFalhou) não responde a um comando: sai do
-- job de consulta ao Mercado Pago ou do webhook simulado. Por isso o billing guarda o
-- sagaId recebido no ComandoCobrarPagamento e publica o resultado com ele.
-- Nulo = cobrança anterior a esta versão ou criada pela API: usa o id da OS.
-- =====================================================================

ALTER TABLE payments ADD COLUMN saga_id VARCHAR(64);
