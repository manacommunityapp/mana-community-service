package com.manacommunity.api.serviceplatform.material.engine;

import com.manacommunity.api.serviceplatform.material.entity.ServiceMaterial;
import com.manacommunity.api.serviceplatform.material.entity.WorkOrderMaterial;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MaterialEngine {

    public BigDecimal calculateMaterialTotal(ServiceMaterial material, int quantity) {
        if (material == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return material.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal calculateTotalMaterialsCost(List<WorkOrderMaterial> materials) {
        if (materials == null || materials.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (WorkOrderMaterial m : materials) {
            sum = sum.add(m.getTotalPrice());
        }
        return sum;
    }

    public void deductStock(ServiceMaterial material, int quantity) {
        if (material.getStockQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for material: " + material.getItemName());
        }
        material.setStockQuantity(material.getStockQuantity() - quantity);
    }
}
