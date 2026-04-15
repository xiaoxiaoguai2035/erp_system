package com.dongjian.erp.manufacturingerpsystem.modules.inventory.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventoryOperationService {

    Long createInStockDoc(String docType,
                          String sourceType,
                          Long sourceId,
                          Long warehouseId,
                          LocalDate bizDate,
                          String remark,
                          List<DocItemParam> items);

    Long createOutStockDoc(String docType,
                           String sourceType,
                           Long sourceId,
                           Long warehouseId,
                           LocalDate bizDate,
                           String remark,
                           List<DocItemParam> items);

    class DocItemParam {
        private Long materialId;
        private String lotNo;
        private BigDecimal qty;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private Long sourceItemId;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public String getLotNo() {
            return lotNo;
        }

        public void setLotNo(String lotNo) {
            this.lotNo = lotNo;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public void setQty(BigDecimal qty) {
            this.qty = qty;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Long getSourceItemId() {
            return sourceItemId;
        }

        public void setSourceItemId(Long sourceItemId) {
            this.sourceItemId = sourceItemId;
        }
    }
}
