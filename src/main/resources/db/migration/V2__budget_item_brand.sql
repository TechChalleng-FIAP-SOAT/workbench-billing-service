-- =====================================================================
-- V2 — Marca da peça no item do orçamento.
-- A mesma peça (ex.: pastilha de freio) pode existir em marcas diferentes,
-- cada uma com seu preço. A marca é um atributo próprio do item, exibido em
-- coluna separada no orçamento real. Nula para itens de mão de obra.
-- =====================================================================

ALTER TABLE budget_items ADD COLUMN brand VARCHAR(255);
