package com.dongjian.erp.manufacturingerpsystem.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InventoryCheckSaveRequest {

    @NotNull(message = "盘点仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "业务日期不能为空")
    private LocalDate bizDate;

    private String remark;

    @Valid
    @NotEmpty(message = "盘点明细不能为空")
    private List<InventoryCheckItemRequest> items;

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

    public List<InventoryCheckItemRequest> getItems() {
        return items;
    }

    public void setItems(List<InventoryCheckItemRequest> items) {
        this.items = items;
    }

    public static class InventoryCheckItemRequest {

        @NotNull(message = "物料不能为空")
        private Long materialId;

        private String lotNo;

        @NotNull(message = "实盘数量不能为空")
        @DecimalMin(value = "0", message = "实盘数量不能小于0")
        private BigDecimal actualQty;

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

        public BigDecimal getActualQty() {
            return actualQty;
        }

        public void setActualQty(BigDecimal actualQty) {
            this.actualQty = actualQty;
        }
    }
}
