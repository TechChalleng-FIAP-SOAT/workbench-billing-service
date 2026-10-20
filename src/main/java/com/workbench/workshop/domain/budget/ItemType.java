package com.workbench.workshop.domain.budget;

/**
 * Tipo de item do orçamento (ver ADR-0005 / PDF real):
 * - PART / SUPPLY: peças e insumos (parte "a" do orçamento).
 * - SERVICE_COST: mão de obra do serviço (parte "b", "Custo MO" no documento real).
 * Total do orçamento = Σ(peças/insumos) + Σ(mão de obra).
 */
public enum ItemType {
    PART, SUPPLY, SERVICE_COST
}
