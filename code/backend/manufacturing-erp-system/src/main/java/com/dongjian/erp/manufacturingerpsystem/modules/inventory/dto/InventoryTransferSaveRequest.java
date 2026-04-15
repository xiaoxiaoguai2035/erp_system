package com.dongjian.erp.manufacturingerpsystem.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InventoryTransferSaveRequest {

    @NotNull(message = "源仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "目标仓库不能为空")
    private Long targetWarehouseId;

    @NotNull(message = "业务日期不能为空")
    private LocalDate bizDate;

    private String remark;

    @Valid
    @NotEmpty(message = "调拨明细不能为空")
    private List<InventoryTransferItemRequest> items;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getTargetWarehouseId() {
        return targetWarehouseId;
    }

    public void setTargetWarehouseId(Long targetWarehouseId) {
        this.targetWarehouseId = targetWarehouseId;
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

    public List<InventoryTransferItemRequest> getItems() {
        return items;
    }

    public void setItems(List<InventoryTransferItemRequest> items) {
        this.items = items;
    }

    public static class InventoryTransferItemRequest {

        @NotNull(message = "物料不能为空")
        private Long materialId;

        private String lotNo;

        @NotNull(message = "调拨数量不能为空")
        @DecimalMin(value = "0.0001", message = "调拨数量必须大于0")
        private BigDecimal qty;

        @DecimalMin(value = "0", message = "单价不能小于0")
        private BigDecimal unitPrice;

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
    }
}
