-- =====================================================================
-- V7 — Documento (CPF/CNPJ, só dígitos) do cliente dono do orçamento.
-- Vem do OS Service no ComandoGerarOrcamento e permite que um token de
-- cliente consulte só o próprio orçamento/pagamento. Nulo em orçamentos
-- antigos: nesse caso só o staff consulta.
-- =====================================================================

ALTER TABLE budgets ADD COLUMN customer_document VARCHAR(14);
