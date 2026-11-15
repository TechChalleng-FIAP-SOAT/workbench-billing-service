-- =====================================================================
-- V6 — Tabela do ShedLock (lock distribuído para jobs @Scheduled).
-- Com múltiplas réplicas (HPA), só uma executa o job de consulta de
-- pagamentos ao Mercado Pago por janela.
-- =====================================================================

CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
