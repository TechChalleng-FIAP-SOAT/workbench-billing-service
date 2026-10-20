package com.workbench.workshop.domain.budget;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetItem {
    private Long id;
    private Long budgetId;
    private String productId;      // id do produto no Execution (ObjectId); nulo para MO
    private String name;           // nome da peça ou descrição da MO
    private String sku;            // sku da peça; para MO, o "código MO" (opcional)
    private String brand;          // marca da peça (diferencia preço); nulo para MO
    private ItemType itemType;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    /** Snapshot de peça/insumo: congela o preço de venda × quantidade. */
    public static BudgetItem partSnapshot(String productId, String name, String sku, String brand,
                                          ItemType type, BigDecimal salePrice, BigDecimal quantity) {
        BudgetItem item = new BudgetItem();
        item.productId = productId;
        item.name = name;
        item.sku = sku;
        item.brand = brand;
        item.itemType = type;        // PART ou SUPPLY
        item.quantity = quantity;
        item.unitPrice = salePrice;
        item.totalPrice = salePrice.multiply(quantity);
        return item;
    }

    /** Snapshot de mão de obra: custo fixo do serviço (quantidade 1). */
    public static BudgetItem laborSnapshot(String serviceName, String laborCode, BigDecimal laborCost) {
        BudgetItem item = new BudgetItem();
        item.name = serviceName;
        item.sku = laborCode;
        item.itemType = ItemType.SERVICE_COST;
        item.quantity = BigDecimal.ONE;
        item.unitPrice = laborCost;
        item.totalPrice = laborCost;
        return item;
    }

    public boolean isLabor() {
        return itemType == ItemType.SERVICE_COST;
    }
}
