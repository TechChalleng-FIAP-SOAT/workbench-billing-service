-- =====================================================================
-- Billing Service — renomeia o status de pagamento APROVADO -> PAGAMENTO_APROVADO
-- (clareza: evita confusão com o status APROVADO da Ordem de Serviço).
-- =====================================================================

-- payments.status tem CHECK constraint que precisa ser recriado com o novo valor.
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;

UPDATE payments        SET status = 'PAGAMENTO_APROVADO' WHERE status = 'APROVADO';
UPDATE payment_history SET status = 'PAGAMENTO_APROVADO' WHERE status = 'APROVADO';

ALTER TABLE payments
    ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDENTE','PAGAMENTO_APROVADO','RECUSADO','ESTORNADO'));
