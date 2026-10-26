-- =====================================================================
-- Billing Service — guarda o link de checkout (checkoutUrl) do pagamento,
-- para enviar ao cliente por e-mail e expor via API (consulta do pagamento).
-- =====================================================================

ALTER TABLE payments ADD COLUMN checkout_url VARCHAR(1000);
