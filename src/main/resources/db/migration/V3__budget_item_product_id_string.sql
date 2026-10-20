-- =====================================================================
-- V3 — product_id do item do orçamento passa a ser texto.
-- O id do produto é dono do Execution Service (MongoDB, ObjectId textual).
-- Catálogos são independentes entre serviços, então o Billing guarda esse id
-- apenas como referência de rastreio do snapshot — sem semântica numérica.
-- =====================================================================

ALTER TABLE budget_items ALTER COLUMN product_id TYPE VARCHAR(255) USING product_id::VARCHAR;
