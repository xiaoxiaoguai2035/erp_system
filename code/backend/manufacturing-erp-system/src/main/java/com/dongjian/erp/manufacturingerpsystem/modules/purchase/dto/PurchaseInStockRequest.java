package com.dongjian.erp.manufacturingerpsystem.modules.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PurchaseInStockRequest {

    @NotNull(message = "入库仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "业务日期不能为空")
    private LocalDate bizDate;

    private String remark;

    @Valid
    @NotEmpty(message = "入库明细不能为空")
    private List<PurchaseInStockItemRequest> items;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDate getBizDate() {
        return bizDate;
    }

    public void setBizDate(LocalDate bizDate) {
        this.bizDate = bizDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<PurchaseInStockItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PurchaseInStockItemRequest> items) {
        this.items = items;
    }

    public static class PurchaseInStockItemRequest {
        @NotNull(message = "来源明细不能为空")
        private Long sourceItemId;
        @NotNull(message = "物料不能为空")
        private Long materialId;
        private String lotNo;
        @NotNull(message = "到货数量不能为空")
        @DecimalMin(value = "0", message = "到货数量不能小于0")
        private BigDecimal receivedQty;
        @NotNull(message = "合格数量不能为空")
        @DecimalMin(value = "0", message = "合格数量不能小于0")
        private BigDecimal qualifiedQty;
        @NotNull(message = "入库数量不能为空")
        @DecimalMin(value = "0.0001", message = "入库数量必须大于0")
        private BigDecimal qty;

        public Long getSourceItemId() {
            return sourceItemId;
        }

        public void setSourceItemId(Long sourceItemId) {
            this.sourceItemId = sourceItemId;
        }

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

        public BigDecimal getReceivedQty() {
            return receivedQty;
        }

        public void setReceivedQty(BigDecimal receivedQty) {
            this.receivedQty = receivedQty;
        }

        public BigDecimal getQualifiedQty() {
            return qualifiedQty;
        }

        public void setQualifiedQty(BigDecimal qualifiedQty) {
            this.qualifiedQty = qualifiedQty;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public void setQty(BigDecimal qty) {
            this.qty = qty;
        }
    }
}
